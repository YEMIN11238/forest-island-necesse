package necesse.engine.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import necesse.engine.commands.parameterHandlers.ParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.gfx.GameColor;

public class CmdParameter {
   public final String name;
   public final ParameterHandler<?> param;
   public final boolean optional;
   public final boolean partOfUsage;
   public final CmdParameter[] extraParams;

   public CmdParameter(String name, ParameterHandler<?> param, boolean optional, boolean partOfUsage, CmdParameter... extraParams) {
      this.name = name;
      this.param = param;
      this.optional = optional;
      this.partOfUsage = partOfUsage;
      this.extraParams = extraParams;
   }

   public CmdParameter(String name, ParameterHandler<?> param, CmdParameter... extraParams) {
      this(name, param, true, true, extraParams);
   }

   public CmdParameter(String name, ParameterHandler<?> param, boolean optional, CmdParameter... extraParams) {
      this(name, param, optional, true, extraParams);
   }

   public CmdParameter(String name, ParameterHandler<?> param) {
      this(name, param, false);
   }

   public String getUsage() {
      if (!this.partOfUsage) {
         return null;
      }

      String out = "";
      if (this.optional) {
         out = out + "[";
      }

      out = out + "<" + this.name + ">";
      if (this.extraParams.length > 0) {
         String extraUsage = getUsage(this.extraParams);
         if (!extraUsage.isEmpty()) {
            out = out + " " + extraUsage;
         }
      }

      if (this.optional) {
         out = out + "]";
      }

      return out;
   }

   public boolean parse(
      Client client,
      Server server,
      ServerClient serverClient,
      ArrayList<String> args,
      ArrayList<Object> parses,
      ArrayList<String> errors,
      CmdParameter.ArgCounter argCounter,
      CommandLog logs
   ) {
      try {
         if (args.isEmpty()) {
            errors.add(null);
            if (this.optional) {
               this.addDefaults(client, server, serverClient, parses, errors);
               return true;
            } else {
               logs.add("Missing argument <" + this.name + ">");
               return false;
            }
         } else {
            String arg = "";
            int useArgs = this.param.getArgsUsed();

            for (int i = 0; i < useArgs; i++) {
               if (i >= args.size()) {
                  useArgs = i;
                  break;
               }

               arg = arg + args.get(i);
               if (i < useArgs - 1) {
                  arg = arg + " ";
               }
            }

            Object parse = this.param.parse(client, server, serverClient, arg, this);
            argCounter.currentArg += useArgs;
            argCounter.currentParam = argCounter.currentParam + this.countParameters();

            for (int i = 0; i < useArgs; i++) {
               args.remove(0);
            }

            parses.add(parse);
            errors.add(null);

            for (CmdParameter param : this.extraParams) {
               if (!param.parse(client, server, serverClient, args, parses, errors, argCounter, logs)) {
                  return false;
               }
            }

            return true;
         }
      } catch (IllegalArgumentException ex) {
         errors.add(ex.getMessage());
         if (this.optional) {
            if (argCounter.params - argCounter.currentParam - this.countParameters(true) <= argCounter.totalArgs - argCounter.currentArg) {
               logs.add(ex.getMessage());
               return false;
            } else {
               argCounter.currentParam = argCounter.currentParam + this.countParameters();
               this.addDefaults(client, server, serverClient, parses, errors);
               return true;
            }
         } else {
            logs.add(ex.getMessage());
            return false;
         }
      }
   }

   private void addDefaults(Client client, Server server, ServerClient serverClient, ArrayList<Object> parses, ArrayList<String> errors) {
      parses.add(this.param.getDefault(client, server, serverClient, this));

      for (CmdParameter param : this.extraParams) {
         errors.add(null);
         param.addDefaults(client, server, serverClient, parses, errors);
      }
   }

   public ArrayList<CmdArgument> getTypingParameters(
      Client client, Server server, ServerClient serverClient, String[] args, CmdParameter.UsageCounter usageCounter
   ) {
      ArrayList<CmdArgument> out = new ArrayList<>();
      boolean done = false;

      for (int optionalArgs = 0; optionalArgs <= usageCounter.optionalArgs; optionalArgs++) {
         int currentStartArg = usageCounter.currentArg - optionalArgs;
         int usedArgs = this.param.getArgsUsed();
         if (currentStartArg < args.length) {
            String arg = "";

            for (int j = 0; j < usedArgs + optionalArgs; j++) {
               int argIndex = currentStartArg + j;
               if (argIndex >= args.length) {
                  usedArgs = j;
                  break;
               }

               arg = arg + args[argIndex] + " ";
            }

            if (arg.endsWith(" ")) {
               arg = arg.substring(0, arg.length() - 1);
            }

            if (this.param.tryParse(client, server, serverClient, arg, this)) {
               usageCounter.currentArg += usedArgs;
               if (this.optional) {
                  usageCounter.optionalArgs += usedArgs;
               }

               if (usageCounter.currentArg < args.length) {
                  out.addAll(getCurrentArguments(client, server, serverClient, this.extraParams, args, usageCounter));
                  done = false;
               } else {
                  if (!this.optional) {
                     done = true;
                  }

                  out.add(new CmdArgument(this, arg, usedArgs));
               }
               break;
            }
         }
      }

      if (done) {
         usageCounter.currentArg = 1000000;
      }

      return out;
   }

   public int countParameters() {
      return this.countParameters(false);
   }

   public int countParameters(boolean onlyRequired) {
      if (onlyRequired && this.optional) {
         return 0;
      }

      int out = this.param.getArgsUsed();

      for (CmdParameter param : this.extraParams) {
         out += param.countParameters(onlyRequired);
      }

      return out;
   }

   public static String getUsage(CmdParameter[] params) {
      StringBuilder out = new StringBuilder();

      for (int i = 0; i < params.length; i++) {
         String paramUsage = params[i].getUsage();
         if (paramUsage != null && paramUsage.length() > 0) {
            if (i > 0) {
               out.append(" ");
            }

            out.append(paramUsage);
         }
      }

      return out.toString();
   }

   public static ArrayList<CmdArgument> getCurrentArguments(
      Client client, Server server, ServerClient serverClient, CmdParameter[] parameters, String[] args, CmdParameter.UsageCounter usageCounter
   ) {
      ArrayList<CmdArgument> out = new ArrayList<>();

      for (CmdParameter param : parameters) {
         out.addAll(param.getTypingParameters(client, server, serverClient, args, usageCounter));
      }

      return out;
   }

   public static List<AutoComplete> autoComplete(Client client, Server server, ServerClient serverClient, CmdParameter[] parameters, String[] args) {
      CmdParameter.UsageCounter usageCounter = new CmdParameter.UsageCounter();
      ArrayList<AutoComplete> out = new ArrayList<>();

      for (CmdArgument currentArgument : getCurrentArguments(client, server, serverClient, parameters, args, usageCounter)) {
         out.addAll(currentArgument.param.param.autocomplete(client, server, serverClient, currentArgument));
      }

      return out;
   }

   public static String getCurrentUsage(ChatCommand cmd, Client client, Server server, ServerClient serverClient, CmdParameter[] parameters, String[] args) {
      CmdParameter.UsageCounter usageCounter = new CmdParameter.UsageCounter();
      LinkedList<CmdParameter> typingParameters = new LinkedList<>();

      for (CmdArgument currentArgument : getCurrentArguments(client, server, serverClient, parameters, args, usageCounter)) {
         typingParameters.add(currentArgument.param);
      }

      return "Usage: /" + cmd.name + getCurrentUsage(parameters, typingParameters);
   }

   public static String getCurrentUsage(CmdParameter[] parameters, Collection<CmdParameter> typingParameters) {
      StringBuilder out = new StringBuilder();

      for (CmdParameter parameter : parameters) {
         StringBuilder paramUsage = new StringBuilder();
         if (typingParameters != null && typingParameters.contains(parameter)) {
            paramUsage.append(GameColor.YELLOW.getColorCode()).append(parameter.name).append(GameColor.NO_COLOR.getColorCode());
         } else {
            paramUsage.append(parameter.name);
         }

         paramUsage.append(getCurrentUsage(parameter.extraParams, typingParameters));
         if (!out.toString().endsWith(" ")) {
            out.append(" ");
         }

         if (parameter.optional) {
            out.append("[").append(paramUsage).append("]");
         } else {
            out.append(paramUsage);
         }
      }

      return out.toString();
   }

   public static class ArgCounter {
      public final int params;
      public final int totalArgs;
      public int currentArg;
      public int currentParam;

      public ArgCounter(int params, int totalArgs) {
         this.params = params;
         this.totalArgs = totalArgs;
         this.currentArg = 0;
      }
   }

   public static class UsageCounter {
      public int currentArg;
      public int optionalArgs;
   }
}
