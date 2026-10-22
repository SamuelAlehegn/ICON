package com.eotcicon.domain;

/**
 * Action performed by a moderator on a contribution. The values must stay in
 * sync with the {@code ck_contribution_review_action} check constraint of the
 * schema.
 */
public enum ReviewAction {
	SUBMIT,
	APPROVE,
	REJECT,
	REQUEST_CORRECTION,
	COMMENT,
	WITHDRAW

}
