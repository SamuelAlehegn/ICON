package com.eotcicon.domain;

import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the entity mapping contract against the local H2 mirror of the
 * PostgreSQL schema.
 */
@ActiveProfiles("local")
class EntityMappingTest extends AbstractEntityMappingTest {

}
