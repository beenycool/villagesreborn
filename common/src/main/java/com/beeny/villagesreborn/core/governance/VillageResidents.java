package com.beeny.villagesreborn.core.governance;

import java.util.*;

/**
 * Manages village residents data
 */
public class VillageResidents {
    private final Map<UUID, ResidentData> residents = new HashMap<>();
    private final Map<UUID, String> residentNames = new HashMap<>();
    private final Random random = new Random();
    
    public VillageResidents() {
        // Constructor without default residents initialization
    }
    
    public ResidentData getResident(UUID residentId) {
        ResidentData resident = residents.get(residentId);
        if (resident == null) {
            // Create a new resident if not found
            resident = new ResidentData(residentId);
            
            // Set a default name if we have one stored
            String name = residentNames.get(residentId);
            if (name != null) {
                resident.setName(name);
            } else {
                resident.setName("Resident " + residentId.toString().substring(0, 8));
            }
            
            // Set default values
            resident.setReputation(50 + random.nextInt(50)); // 50-100 reputation
            resident.setAge(18 + random.nextInt(62)); // 18-80 years old
            
            residents.put(residentId, resident);
        }
        return resident;
    }
    
    /**
     * Adds a new resident to the village
     */
    public void addResident(ResidentData resident) {
        if (resident != null && resident.getId() != null) {
            residents.put(resident.getId(), resident);
            if (resident.getName() != null) {
                residentNames.put(resident.getId(), resident.getName());
            }
        }
    }
    
    /**
     * Removes a resident from the village
     */
    public boolean removeResident(UUID residentId) {
        ResidentData removed = residents.remove(residentId);
        residentNames.remove(residentId);
        return removed != null;
    }
    
    /**
     * Gets all residents in the village
     */
    public List<ResidentData> getAllResidents() {
        return new ArrayList<>(residents.values());
    }
    
    /**
     * Gets the number of residents in the village
     */
    public int getResidentCount() {
        return residents.size();
    }
    
    /**
     * Checks if a resident exists in the village
     */
    public boolean hasResident(UUID residentId) {
        return residents.containsKey(residentId);
    }
    
    /**
     * Updates a resident's data
     */
    public void updateResident(ResidentData resident) {
        if (resident != null && resident.getId() != null) {
            residents.put(resident.getId(), resident);
            if (resident.getName() != null) {
                residentNames.put(resident.getId(), resident.getName());
            }
        }
    }
    
    /**
     * Initializes some default residents for testing purposes
     */
    private void initializeDefaultResidents() {
        // Create a few default residents with known names
        String[] defaultNames = {"Alice Cooper", "Bob Builder", "Charlie Farmer", "Diana Merchant"};
        
        for (String name : defaultNames) {
            UUID id = UUID.randomUUID();
            ResidentData resident = new ResidentData(id);
            resident.setName(name);
            resident.setReputation(60 + (int)(Math.random() * 40)); // 60-100 reputation
            resident.setAge(25 + (int)(Math.random() * 40)); // 25-65 years old
            
            residents.put(id, resident);
            residentNames.put(id, name);
        }
    }
}