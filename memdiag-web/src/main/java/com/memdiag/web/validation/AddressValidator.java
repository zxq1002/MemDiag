package com.memdiag.web.validation;

import java.util.regex.Pattern;

public class AddressValidator {

    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    );

    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    private static final Pattern LOCALHOST_PATTERN = Pattern.compile(
        "^localhost$",
        Pattern.CASE_INSENSITIVE
    );

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private AddressValidator() {
    }

    public static boolean isValid(String address) {
        if (address == null || address.trim().isEmpty()) {
            return false;
        }

        String trimmedAddress = address.trim();

        int colonIndex = trimmedAddress.lastIndexOf(':');
        if (colonIndex <= 0 || colonIndex == trimmedAddress.length() - 1) {
            return false;
        }

        String hostPart = trimmedAddress.substring(0, colonIndex);
        String portPart = trimmedAddress.substring(colonIndex + 1);

        if (!isValidHost(hostPart)) {
            return false;
        }

        return isValidPort(portPart);
    }

    private static boolean isValidHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            return false;
        }

        String trimmedHost = host.trim();

        if (LOCALHOST_PATTERN.matcher(trimmedHost).matches()) {
            return true;
        }

        if (IP_PATTERN.matcher(trimmedHost).matches()) {
            return true;
        }

        return HOSTNAME_PATTERN.matcher(trimmedHost).matches();
    }

    private static boolean isValidPort(String portStr) {
        if (portStr == null || portStr.trim().isEmpty()) {
            return false;
        }

        try {
            int port = Integer.parseInt(portStr.trim());
            return port >= MIN_PORT && port <= MAX_PORT;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String getErrorMessage(String address) {
        if (address == null || address.trim().isEmpty()) {
            return "Address cannot be empty";
        }

        String trimmedAddress = address.trim();

        int colonIndex = trimmedAddress.lastIndexOf(':');
        if (colonIndex <= 0) {
            return "Address must be in format 'host:port'";
        }

        if (colonIndex == trimmedAddress.length() - 1) {
            return "Port number is required after colon";
        }

        String hostPart = trimmedAddress.substring(0, colonIndex);
        String portPart = trimmedAddress.substring(colonIndex + 1);

        if (!isValidHost(hostPart)) {
            return "Invalid hostname or IP address: " + hostPart;
        }

        if (!isValidPort(portPart)) {
            return "Port must be a number between " + MIN_PORT + " and " + MAX_PORT;
        }

        return "Invalid address format";
    }
}
