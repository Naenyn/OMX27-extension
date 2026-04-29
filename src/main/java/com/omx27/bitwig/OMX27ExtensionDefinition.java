package com.omx27.bitwig;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class OMX27ExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("aeb3af5c-af0a-42d4-a40c-9fdfe69f6b2b");

   @Override
   public String getName()
   {
      return "OMX-27";
   }

   @Override
   public String getAuthor()
   {
      return "naenyn";
   }

   @Override
   public String getVersion()
   {
      return "0.1.0";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }

   @Override
   public String getHardwareVendor()
   {
      return "okyeron";
   }

   @Override
   public String getHardwareModel()
   {
      return "OMX-27";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 21;
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 1;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      list.add(new String[] {"omx-27-v3"}, new String[] {"omx-27-v3"});
   }

   @Override
   public OMX27Extension createInstance(final ControllerHost host)
   {
      return new OMX27Extension(this, host);
   }
}
