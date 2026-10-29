package com.eotcicon.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import com.eotcicon.domain.*;
import com.eotcicon.domain.enums.RecordStatus;
import com.eotcicon.domain.enums.UserRole;
import com.eotcicon.repository.*;
import com.eotcicon.service.CurrentUserService;
import com.eotcicon.web.dto.auth.UserProfileDto;
import com.eotcicon.web.dto.common.PageResponse;
import com.eotcicon.web.dto.stats.ArchiveStatsDto;
import com.eotcicon.service.StatisticsService;
import com.eotcicon.web.dto.church.ChurchSummaryDto;
import com.eotcicon.web.dto.saint.SaintSummaryDto;
import com.eotcicon.web.mapper.ChurchMapper;
import com.eotcicon.web.mapper.SaintMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/admin")
@Transactional
public class AdminManagementController {
    private final ChurchRepository churches;
    private final SaintRepository saints;
    private final UserRepository users;
    private final IconRepository icons;
    private final IconSubjectRepository subjects;
    private final ChurchAliasRepository churchAliases;
    private final SaintAliasRepository saintAliases;
    private final AuditLogRepository auditLogs;
    private final CurrentUserService currentUser;
    private final StatisticsService statistics;

    public AdminManagementController(ChurchRepository churches, SaintRepository saints, UserRepository users,
            IconRepository icons, IconSubjectRepository subjects, ChurchAliasRepository churchAliases,
            SaintAliasRepository saintAliases, AuditLogRepository auditLogs, CurrentUserService currentUser,
            StatisticsService statistics) {
        this.churches = churches; this.saints = saints; this.users = users; this.icons = icons;
        this.subjects = subjects; this.churchAliases = churchAliases; this.saintAliases = saintAliases;
        this.auditLogs = auditLogs; this.currentUser = currentUser; this.statistics = statistics;
    }

    public record ChurchRequest(@NotBlank String name, String region, String town, String country,
            BigDecimal latitude, BigDecimal longitude, String description) {}
    public record SaintRequest(@NotBlank String canonicalName, String amharicName, String geezName,
            String category, String description, boolean verified) {}
    public record MergeRequest(UUID targetId) {}
    public record RoleRequest(UserRole role) {}
    public record AuditEntry(UUID id, Instant createdAt, UUID actorId, String actorName, String action,
            String entityType, UUID entityId, String detail, String ipAddress) {}

    @GetMapping("/statistics")
    public ArchiveStatsDto getStatistics() { return statistics.getStatistics(); }

    @GetMapping("/users")
    public PageResponse<UserProfileDto> getUsers(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="25") int size) {
        Page<User> result = users.findAll(PageRequest.of(Math.max(page,0), Math.min(Math.max(size,1),100)));
        return PageResponse.of(result, u -> new UserProfileDto(u.getId(), u.getEmail(), u.getDisplayName(),
                u.getRole(), u.getStatus(), u.getLastLoginAt(), u.getCreatedAt()));
    }

    @PatchMapping("/users/{id}/role")
    public UserProfileDto setRole(@PathVariable UUID id, @RequestBody RoleRequest request) {
        if (request.role() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        User user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserRole oldRole = user.getRole(); user.setRole(request.role()); users.save(user);
        audit("USER_ROLE_CHANGED", "USER", id, oldRole + " → " + request.role());
        return new UserProfileDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(),
                user.getStatus(), user.getLastLoginAt(), user.getCreatedAt());
    }

    @GetMapping("/audit-log")
    public PageResponse<AuditEntry> getAuditLog(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="25") int size) {
        Page<AuditLog> result = auditLogs.findAll(PageRequest.of(Math.max(page,0), Math.min(Math.max(size,1),100),
                org.springframework.data.domain.Sort.by("createdAt").descending()));
        return PageResponse.of(result, a -> new AuditEntry(a.getId(), a.getCreatedAt(),
                a.getActor() == null ? null : a.getActor().getId(), a.getActor() == null ? "System" : a.getActor().getDisplayName(),
                a.getAction(), a.getEntityType(), a.getEntityId(), a.getDetail(), a.getIpAddress()));
    }

    @PostMapping("/churches")
    public ChurchSummaryDto createChurch(@Valid @RequestBody ChurchRequest request) {
        Church church = new Church(); apply(church, request); church.setSlug(uniqueSlug(request.name()));
        church.setStatus(RecordStatus.ACTIVE); churches.save(church); audit("CHURCH_CREATED", "CHURCH", church.getId(), church.getName()); return ChurchMapper.toSummary(church);
    }
    @PutMapping("/churches/{id}")
    public ChurchSummaryDto updateChurch(@PathVariable UUID id, @Valid @RequestBody ChurchRequest request) {
        Church church = churches.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        apply(church, request); churches.save(church); audit("CHURCH_UPDATED", "CHURCH", id, church.getName()); return ChurchMapper.toSummary(church);
    }
    @PostMapping("/churches/{id}/merge")
    public void mergeChurch(@PathVariable UUID id, @RequestBody MergeRequest request) {
        if (request == null || request.targetId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A target church is required");
        Church source = churches.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Church target = churches.findById(request.targetId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (source.equals(target)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a different target");
        icons.findAll().stream().filter(i -> source.equals(i.getChurch())).forEach(i -> i.setChurch(target));
        churchAliases.findAll().stream().filter(a -> source.equals(a.getChurch())).forEach(a -> a.setChurch(target));
        source.setStatus(RecordStatus.MERGED); source.setMergedInto(target);
        churches.save(source); churches.save(target); audit("CHURCH_MERGED", "CHURCH", id, "Merged into " + target.getId());
    }

    @PostMapping("/saints")
    public SaintSummaryDto createSaint(@Valid @RequestBody SaintRequest request) {
        Saint saint = new Saint(); apply(saint, request); saint.setSlug(uniqueSlug(request.canonicalName()));
        saint.setStatus(RecordStatus.ACTIVE); saints.save(saint); audit("SAINT_CREATED", "SAINT", saint.getId(), saint.getCanonicalName()); return SaintMapper.toSummary(saint);
    }
    @PutMapping("/saints/{id}")
    public SaintSummaryDto updateSaint(@PathVariable UUID id, @Valid @RequestBody SaintRequest request) {
        Saint saint = saints.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        apply(saint, request); saints.save(saint); audit("SAINT_UPDATED", "SAINT", id, saint.getCanonicalName()); return SaintMapper.toSummary(saint);
    }
    @PostMapping("/saints/{id}/merge")
    public void mergeSaint(@PathVariable UUID id, @RequestBody MergeRequest request) {
        if (request == null || request.targetId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A target saint is required");
        Saint source = saints.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Saint target = saints.findById(request.targetId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (source.equals(target)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a different target");
        subjects.findAll().stream().filter(s -> source.equals(s.getSaint())).forEach(s -> s.setSaint(target));
        saintAliases.findAll().stream().filter(a -> source.equals(a.getSaint())).forEach(a -> a.setSaint(target));
        source.setStatus(RecordStatus.MERGED); source.setMergedInto(target);
        saints.save(source); saints.save(target); audit("SAINT_MERGED", "SAINT", id, "Merged into " + target.getId());
    }

    private void apply(Church c, ChurchRequest r) { c.setName(r.name().trim()); c.setRegion(r.region()); c.setTown(r.town()); c.setCountry(r.country() == null || r.country().isBlank() ? "Ethiopia" : r.country()); c.setLatitude(r.latitude()); c.setLongitude(r.longitude()); c.setDescription(r.description()); }
    private void apply(Saint s, SaintRequest r) { s.setCanonicalName(r.canonicalName().trim()); s.setAmharicName(r.amharicName()); s.setGeezName(r.geezName()); s.setCategory(r.category()); s.setDescription(r.description()); s.setVerified(r.verified()); }
    private String uniqueSlug(String value) {
        String base = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (base.isBlank()) base = "record";
        String candidate = base; int suffix = 2;
        while (churches.existsBySlug(candidate) || saints.existsBySlug(candidate)) candidate = base + "-" + suffix++;
        return candidate;
    }
    private void audit(String action, String type, UUID id, String detail) {
        AuditLog entry = new AuditLog(); entry.setActor(currentUser.requireCurrentUser()); entry.setAction(action);
        entry.setEntityType(type); entry.setEntityId(id); entry.setDetail(detail); auditLogs.save(entry);
    }
}
