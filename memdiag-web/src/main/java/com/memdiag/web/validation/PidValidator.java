package com.memdiag.web.validation;

public class PidValidator {

    private static final int MIN_PID = 1;
    private static final int MAX_PID = 65535;

    private PidValidator() {
    }

    public static boolean isValid(String pid) {
        if (pid == null || pid.trim().isEmpty()) {
            return false;
        }

        try {
            int pidValue = Integer.parseInt(pid.trim());
            return pidValue >= MIN_PID && pidValue <= MAX_PID;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String getErrorMessage(String pid) {
        if (pid == null || pid.trim().isEmpty()) {
            return "PID cannot be empty";
        }

        try {
            int pidValue = Integer.parseInt(pid.trim());
            if (pidValue < MIN_PID || pidValue > MAX_PID) {
                return "PID must be between " + MIN_PID + " and " + MAX_PID;
            }
            return "Invalid PID format";
        } catch (NumberFormatException e) {
            return "PID must be a numeric value";
        }
    }
}
