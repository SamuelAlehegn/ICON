package com.eotcicon.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.eotcicon.domain.Church;
import com.eotcicon.domain.ChurchAlias;
import com.eotcicon.domain.Saint;
import com.eotcicon.domain.SaintAlias;
import com.eotcicon.domain.User;
import com.eotcicon.domain.enums.RecordStatus;
import com.eotcicon.domain.enums.UserRole;
import com.eotcicon.domain.enums.UserStatus;
import com.eotcicon.web.dto.auth.UserProfileDto;
import com.eotcicon.web.dto.church.ChurchDetailDto;
import com.eotcicon.web.dto.church.ChurchMarkerDto;
import com.eotcicon.web.dto.church.ChurchSummaryDto;
import com.eotcicon.web.dto.icon.IconSummaryDto;
import com.eotcicon.web.dto.saint.SaintDetailDto;
import com.eotcicon.web.dto.saint.SaintSummaryDto;
import com.eotcicon.web.dto.stats.ArchiveStatsDto;

class ChurchSaintMapperTest {

	private static final UUID CHURCH_ID = UUID.randomUUID();
	private static final UUID SAINT_ID = UUID.randomUUID();
	private static final UUID USER_ID = UUID.randomUUID();

	@Test
	void mapsChurchSummaryDetailAndMarker() {
		Church church = church();
		IconSummaryDto icon = new IconSummaryDto(UUID.randomUUID(), "icon", "Icon", null, null,
				List.of(), church.getName(), church.getSlug(), church.getTown(), church.getRegion(),
				church.getCountry(), "Contributor", "thumb", "full", "alt");

		ChurchSummaryDto summary = ChurchMapper.toSummary(church);
		ChurchDetailDto detail = ChurchMapper.toDetail(church, List.of(icon));
		ChurchMarkerDto marker = ChurchMapper.toMarker(church, 7);

		assertThat(summary.id()).isEqualTo(CHURCH_ID);
		assertThat(summary.name()).isEqualTo("Bete Giyorgis");
		assertThat(summary.latitude()).isEqualByComparingTo("12.031700");
		assertThat(summary.longitude()).isEqualByComparingTo("39.041200");
		assertThat(detail.description()).isEqualTo("A rock-hewn church.");
		assertThat(detail.status()).isEqualTo(RecordStatus.ACTIVE);
		assertThat(detail.aliases()).containsExactly("Beta Giyorgis", "House of Saint George");
		assertThat(detail.iconCount()).isEqualTo(1);
		assertThat(detail.icons()).containsExactly(icon);
		assertThat(marker.id()).isEqualTo(CHURCH_ID);
		assertThat(marker.iconCount()).isEqualTo(7);
	}

	@Test
	void mapsSaintSummaryAndDetailIncludingAmharicAndGeezNames() {
		Saint saint = saint();

		SaintSummaryDto summary = SaintMapper.toSummary(saint, 4L);
		SaintDetailDto detail = SaintMapper.toDetail(saint, List.of(), List.of(ChurchMapper.toSummary(church())));

		assertThat(summary.id()).isEqualTo(SAINT_ID);
		assertThat(summary.canonicalName()).isEqualTo("Saint George");
		assertThat(summary.amharicName()).isEqualTo("ጊዮርጊስ");
		assertThat(summary.geezName()).isEqualTo("ጊዮርጊስ");
		assertThat(summary.verified()).isTrue();
		assertThat(summary.iconCount()).isEqualTo(4L);
		assertThat(detail.description()).isEqualTo("A warrior saint.");
		assertThat(detail.status()).isEqualTo(RecordStatus.ACTIVE);
		assertThat(detail.aliases()).containsExactly("Saint Giorgis");
		assertThat(detail.iconCount()).isZero();
		assertThat(detail.churches()).hasSize(1);
	}

	@Test
	void mapsArchiveStatsAndUserProfile() {
		ArchiveStatsDto stats = ArchiveStatsMapper.toDto(12, 4, 6, 3, 2, true);
		User user = user();

		UserProfileDto profile = UserMapper.toProfile(user);

		assertThat(stats.publishedIcons()).isEqualTo(12);
		assertThat(stats.churches()).isEqualTo(4);
		assertThat(stats.saints()).isEqualTo(6);
		assertThat(stats.regions()).isEqualTo(3);
		assertThat(stats.contributors()).isEqualTo(2);
		assertThat(stats.sampleData()).isTrue();
		assertThat(profile.id()).isEqualTo(USER_ID);
		assertThat(profile.email()).isEqualTo("moderator@example.org");
		assertThat(profile.displayName()).isEqualTo("Moderator");
		assertThat(profile.role()).isEqualTo(UserRole.MODERATOR);
		assertThat(profile.status()).isEqualTo(UserStatus.ACTIVE);
	}

	private static Church church() {
		Church church = new Church();
		ReflectionTestUtils.setField(church, "id", CHURCH_ID);
		ReflectionTestUtils.setField(church, "createdAt", Instant.parse("2024-01-01T00:00:00Z"));
		ReflectionTestUtils.setField(church, "updatedAt", Instant.parse("2024-01-02T00:00:00Z"));
		church.setName("Bete Giyorgis");
		church.setSlug("bete-giyorgis");
		church.setRegion("Amhara");
		church.setTown("Lalibela");
		church.setCountry("Ethiopia");
		church.setLatitude(new BigDecimal("12.031700"));
		church.setLongitude(new BigDecimal("39.041200"));
		church.setDescription("A rock-hewn church.");
		church.setStatus(RecordStatus.ACTIVE);

		ChurchAlias firstAlias = new ChurchAlias();
		firstAlias.setChurch(church);
		firstAlias.setName("House of Saint George");
		church.getAliases().add(firstAlias);

		ChurchAlias secondAlias = new ChurchAlias();
		secondAlias.setChurch(church);
		secondAlias.setName("Beta Giyorgis");
		church.getAliases().add(secondAlias);

		return church;
	}

	private static Saint saint() {
		Saint saint = new Saint();
		ReflectionTestUtils.setField(saint, "id", SAINT_ID);
		ReflectionTestUtils.setField(saint, "createdAt", Instant.parse("2024-01-01T00:00:00Z"));
		ReflectionTestUtils.setField(saint, "updatedAt", Instant.parse("2024-01-02T00:00:00Z"));
		saint.setCanonicalName("Saint George");
		saint.setSlug("saint-george");
		saint.setAmharicName("ጊዮርጊስ");
		saint.setGeezName("ጊዮርጊስ");
		saint.setCategory("Warrior saint");
		saint.setDescription("A warrior saint.");
		saint.setVerified(true);
		saint.setStatus(RecordStatus.ACTIVE);

		SaintAlias alias = new SaintAlias();
		alias.setSaint(saint);
		alias.setName("Saint Giorgis");
		saint.getAliases().add(alias);

		return saint;
	}

	private static User user() {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", USER_ID);
		ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2024-01-01T00:00:00Z"));
		user.setEmail("moderator@example.org");
		user.setPasswordHash("secret-hash");
		user.setDisplayName("Moderator");
		user.setRole(UserRole.MODERATOR);
		user.setStatus(UserStatus.ACTIVE);
		user.setLastLoginAt(Instant.parse("2024-03-01T00:00:00Z"));
		return user;
	}

}
