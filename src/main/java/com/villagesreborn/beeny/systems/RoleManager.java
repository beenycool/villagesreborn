package com.villagesreborn.beeny.systems;

import com.villagesreborn.beeny.Villagesreborn;

public class RoleManager {
    private PlayerRole currentRole = PlayerRole.MAYOR; // Default role is Mayor

    public RoleManager() {
        loadPlayerRole();
    }

    public void selectRole(PlayerRole role) {
        this.currentRole = role;
        savePlayerRole(role);
        // Implement role-specific logic here, e.g., update player abilities, etc.
    }

    public PlayerRole getCurrentRole() {
        return currentRole;
    }

    private void savePlayerRole(PlayerRole role) {
        Villagesreborn.playerData.setPlayerRole(role.toString());
    }

    private void loadPlayerRole() {
        String roleName = Villagesreborn.playerData.getPlayerRole();
        if (roleName != null && !roleName.isEmpty()) {
            try {
                currentRole = PlayerRole.valueOf(roleName);
            } catch (IllegalArgumentException e) {
                currentRole = PlayerRole.MAYOR; // Default back to Mayor if role is invalid
            }
        }
    }
}
