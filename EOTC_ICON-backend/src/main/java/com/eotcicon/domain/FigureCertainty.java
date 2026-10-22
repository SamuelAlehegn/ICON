package com.eotcicon.domain;

/**
 * How certain it is that a depicted figure is the referenced saint. The
 * values must stay in sync with the {@code ck_icon_subject_certainty} and
 * {@code ck_icon_certainty} check constraints of the schema.
 */
public enum FigureCertainty {
	IDENTIFIED,
	PROBABLE,
	UNKNOWN

}
