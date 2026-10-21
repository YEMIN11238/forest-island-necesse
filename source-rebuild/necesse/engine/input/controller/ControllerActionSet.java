package necesse.engine.input.controller;

import java.util.ArrayList;
import java.util.HashSet;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputManager;
import necesse.engine.input.InputSource;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public class ControllerActionSet {
   public final String name;
   public final ArrayList<ControllerInputState> states = new ArrayList<>();
   public final HashSet<ControllerInputState> nonOverwrittenStates = new HashSet<>();
   public final GameMessage displayName;
   private boolean initialized = false;

   public ControllerActionSet(String name, GameMessage displayName) {
      this.name = name;
      this.displayName = displayName;
   }

   public void init() {
      if (!this.initialized) {
         this.initialized = true;

         for (ControllerInputState state : this.states) {
            state.init();
         }
      }
   }

   public void addState(ControllerInputState state) {
      this.states.add(state);
      state.setActionSet(this);
   }

   public ControllerHandle updateStates(ArrayList<ControllerEvent> outEvents, TickManager tickManager) {
      ControllerHandle controllerHandle = null;
      ArrayList<InputSource> inputSources = InputManager.getInputSources();
      ControllerHandle latestControllerHandle = ControllerInput.getLatestControllerHandle();

      label29:
      for (ControllerInputState state : this.nonOverwrittenStates) {
         for (InputSource source : inputSources) {
            ControllerHandle handleIfAny = source.updateControllerState(state, outEvents, tickManager);
            if (handleIfAny != null) {
               controllerHandle = handleIfAny;
               continue label29;
            }
         }

         if (state instanceof ControllerButtonState) {
            ((ControllerButtonState)state).updateState(false, latestControllerHandle, null, outEvents, tickManager);
         } else if (state instanceof ControllerAnalogState) {
            ((ControllerAnalogState)state).updateState(0.0F, 0.0F, latestControllerHandle, outEvents);
         }
      }

      return controllerHandle;
   }

   public void updateOverwrittenStates(ArrayList<ControllerBind> usedBinds, ArrayList<ControllerEvent> outEvents, TickManager tickManager) {
      this.nonOverwrittenStates.clear();
      ArrayList<ControllerBind> newUsedBinds = new ArrayList<>(this.states.size());
      ArrayList<InputSource> inputSources = InputManager.getInputSources();
      ControllerHandle latestControllerHandle = ControllerInput.getLatestControllerHandle();

      label48:
      for (ControllerInputState state : this.states) {
         for (InputSource inputSource : inputSources) {
            ControllerBind bind = inputSource.getControllerBindForState(state, latestControllerHandle);
            if (bind != null && bind.isBound()) {
               for (ControllerBind usedBind : usedBinds) {
                  if (bind.equals(usedBind)) {
                     if (state instanceof ControllerButtonState) {
                        ((ControllerButtonState)state).updateState(false, latestControllerHandle, null, outEvents, tickManager);
                     } else if (state instanceof ControllerAnalogState) {
                        ((ControllerAnalogState)state).updateState(0.0F, 0.0F, latestControllerHandle, outEvents);
                     }
                     continue label48;
                  }
               }

               this.nonOverwrittenStates.add(state);
               newUsedBinds.add(bind);
            }
         }
      }

      usedBinds.addAll(newUsedBinds);
   }

   public void saveStateBinds(SaveData saveData, LoadedMod mod) {
      SaveData actionSetSaveData = new SaveData(this.name);

      for (ControllerInputState state : this.states) {
         if (state != null && state.mod == mod) {
            SaveData stateSaveData = new SaveData(state.actionName);

            for (InputSource source : InputManager.getInputSources()) {
               source.saveControllerStateBind(state, stateSaveData);
            }

            if (!stateSaveData.isEmpty()) {
               actionSetSaveData.addSaveData(stateSaveData);
            }
         }
      }

      if (!actionSetSaveData.isEmpty()) {
         saveData.addSaveData(actionSetSaveData);
      }
   }

   public void loadStateBinds(LoadData loadData, LoadedMod mod) {
      LoadData actionSetLoadData = loadData.getFirstLoadDataByName(this.name);
      if (actionSetLoadData != null && !actionSetLoadData.isEmpty()) {
         for (ControllerInputState state : this.states) {
            if (state != null && state.mod == mod) {
               LoadData stateLoadData = actionSetLoadData.getFirstLoadDataByName(state.actionName);
               if (stateLoadData != null && !stateLoadData.isEmpty()) {
                  for (InputSource source : InputManager.getInputSources()) {
                     source.loadControllerStateBind(state, stateLoadData);
                  }
               }
            }
         }
      }
   }
}
