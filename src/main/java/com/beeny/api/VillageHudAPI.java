package com.beeny.api;

public interface VillageHudAPI {
    void updateVillageInfo(String cultureName, int prosperity, int safety, int population);
    void showEventNotification(String title, String description, int durationTicks);
    
    VillageHudAPI DUMMY = new VillageHudAPI() {
        @Override
        public void updateVillageInfo(String cultureName, int prosperity, int safety, int population) {}
        
        @Override
        public void showEventNotification(String title, String description, int durationTicks) {}
    };
    
    static VillageHudAPI getInstance() {
        return DUMMY;
    }
}
