package com.ibarnstormer.projectomnipotence.config;


public record POPlayerConfig(String username, String stringUUID, boolean enlightenedOnStart, int eeHandicap, int eeMultiplier) {

    public boolean isWildcard() {
        return username.equals("*");
    }

}
