package peerpal.model;

import java.util.Optional;

/**
 * All user roles supported by the system.
 * Add or rename roles here to match Section 2 of the System Proposal;
 * registration, the database CHECK constraint and routing all read from this enum.
 */
public enum Role {
    TEACHER,
    STUDENT,
    TUTOR,
    ADMIN;

    /** Safe lookup from the text stored in the database. */
    public static Optional<Role> fromString(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(value.trim())) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }
}
