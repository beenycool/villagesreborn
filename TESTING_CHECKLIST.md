# Villages Reborn Mod - Testing Checklist

## Villager Bank Persistence

- [ ] **Test Case 1: Deposit and Withdraw Persistence**
    1. Spawn a villager.
    2. Deposit items into the villager's bank.
    3. Save and reload the world.
    4. Verify that the deposited items are still in the villager's bank after reload.
    5. Withdraw items from the bank.
    6. Save and reload the world again.
    7. Verify that the withdrawn items are no longer in the bank and were successfully withdrawn.

## Name & Role Refinement

- [ ] **Test Case 2: Name Generation**
    1. Spawn multiple villagers.
    2. Verify that villagers are assigned names from the VILLAGER_FIRST_NAMES and VILLAGER_LAST_NAMES lists.
    3. Check for variety in generated names.

- [ ] **Test Case 3: Role Assignment**
    1. Spawn a large number of villagers.
    2. Observe the distribution of villager roles (Trader, Builder, Defender, Patroller).
    3. Verify that the roles are assigned with the weighted probabilities (Trader: 40%, Builder: 30%, Defender: 20%, Patroller: 10%).

## Patrol Routes

- [ ] **Test Case 4: Patrol Route Following**
    1. Spawn a Patroller villager.
    2. Observe the patroller's movement.
    3. Verify that the patroller moves along a defined patrol route around the village.
    4. Check if the patrol route is dynamically generated based on village location.

## Ranged Attack Behavior

- [ ] **Test Case 5: Ranged Attack Functionality**
    1. Spawn a Patroller villager and equip it with arrows (place arrows in the villager's bank inventory).
    2. Spawn hostile mobs (e.g., zombies, skeletons) near the patroller.
    3. Verify that the patroller engages hostile mobs using ranged attacks (bow and arrows).
    4. Check if the patroller consumes arrows when shooting.
    5. Verify that there is a cooldown between ranged attacks.

## Custom Skins

- [ ] **Test Case 6: Custom Skin Application**
    1. Spawn villagers of different roles (Trader, Builder, Defender, Patroller).
    2. Verify that each villager role has a distinct custom skin applied.
    3. Check if the default skin is applied if a custom skin is not defined or missing.

## General Stability

- [ ] **Test Case 7: No Crashes or Errors**
    1. Play the game with the Villages Reborn mod enabled for an extended period.
    2. Monitor for any crashes, errors, or unexpected behavior.
    3. Check the game logs for any errors related to the new features.

## Test Report

- [ ] **Summary:** Briefly summarize the testing process and overall results.
- [ ] **Pass/Fail Status:** For each test case, indicate whether it passed or failed.
- [ ] **Issues Found:** List any issues or bugs identified during testing.
- [ ] **Recommendations:** Provide recommendations for fixing any issues and further improvements.
- [ ] **Conclusion:** State the overall stability and functionality of the implemented features based on the testing results.

**Tester:** [Your Name]
**Date:** [Date]
**Mod Version:** [Villages Reborn Mod Version]
