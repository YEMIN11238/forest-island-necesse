package necesse.engine.input.controller;

import necesse.engine.input.InputSource;

public abstract class ControllerHandle {
   public final InputSource inputSource;
   public final ControllerHandle.ControllerType type;

   public ControllerHandle(InputSource inputSource, ControllerHandle.ControllerType type) {
      this.inputSource = inputSource;
      this.type = type;
   }

   public enum ControllerType {
      Unknown,
      Xbox,
      PlayStation4,
      PlayStation5,
      XboxGeneric,
      XboxLinuxWireless,
      XboxLinuxWired;
   }
}
