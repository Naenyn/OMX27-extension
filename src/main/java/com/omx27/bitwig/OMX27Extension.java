package com.omx27.bitwig;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.callback.DoubleValueChangedCallback;
import com.bitwig.extension.callback.EnumValueChangedCallback;
import com.bitwig.extension.callback.IntegerValueChangedCallback;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.Transport;

public class OMX27Extension extends ControllerExtension
{
   private static final int DEFAULT_CC_STOP = 102;
   private static final int DEFAULT_CC_PLAY = 103;
   private static final int DEFAULT_CC_RECORD = 104;
   private static final int DEFAULT_CC_METRONOME_TOGGLE = 105;
   private static final int DEFAULT_CC_COUNT_IN = 106;
   private static final int DEFAULT_CC_METRONOME_FLASH = 107;
   private static final int DEFAULT_CC_VISIBLE_METRONOME_TOGGLE = 108;
   private static final int[] DEFAULT_REMOTE_KNOB_CCS = {110, 111, 112, 113, 114, 115, 116, 117};
   private static final int DEFAULT_CC_SEND1 = 126;
   private static final int DEFAULT_CC_SEND2 = 127;
   private static final int DEFAULT_CC_SEND3 = 100;
   private static final int DEFAULT_CC_PAN = 101;
   private static final int SEND_COUNT = 3;
   private static final int DEFAULT_VOLUME_KNOB_CC = 7;
   private static final int MIDI_CHANNEL_1_CC_STATUS = 0xB0;
   private static final int MIDI_ON = 127;
   private static final int MIDI_OFF = 0;
   private static final int METRONOME_FLASH_OFF_BEAT = 20;
   private static final int MIDI_CC_RANGE = 128;
   private static final int REMOTE_KNOB_COUNT = 8;

   private Transport transport;
   private CursorTrack cursorTrack;
   private CursorRemoteControlsPage remoteControlsPage;
   private MidiOut midiOut;
   private NoteInput noteInput;
   private int stopCc = DEFAULT_CC_STOP;
   private int playCc = DEFAULT_CC_PLAY;
   private int recordCc = DEFAULT_CC_RECORD;
   private int metronomeToggleCc = DEFAULT_CC_METRONOME_TOGGLE;
   private int countInCc = DEFAULT_CC_COUNT_IN;
   private int metronomeFlashCc = DEFAULT_CC_METRONOME_FLASH;
   private int visibleMetronomeToggleCc = DEFAULT_CC_VISIBLE_METRONOME_TOGGLE;
   private final int[] remoteKnobCcs = DEFAULT_REMOTE_KNOB_CCS.clone();
   private int volumeKnobCc = DEFAULT_VOLUME_KNOB_CC;
   private int send1Cc = DEFAULT_CC_SEND1;
   private int send2Cc = DEFAULT_CC_SEND2;
   private int send3Cc = DEFAULT_CC_SEND3;
   private int panCc = DEFAULT_CC_PAN;
   private int timeSignatureNumerator = 4;
   private int timeSignatureDenominator = 4;
   private long lastPulseBeatNumber = Long.MIN_VALUE;
   private boolean isPlaying = false;
   private boolean isVisibleMetronomeEnabled = false;

   protected OMX27Extension(final OMX27ExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = (ControllerHost) getHost();
      transport = host.createTransport();
      cursorTrack = host.createCursorTrack("omx-27-cursor", "OMX-27 Cursor", SEND_COUNT, 0, true);
      remoteControlsPage = cursorTrack.createCursorRemoteControlsPage(REMOTE_KNOB_COUNT);
      midiOut = host.getMidiOutPort(0);
      noteInput = host.getMidiInPort(0).createNoteInput(
         "OMX-27 Notes",
         "8?????",
         "9?????",
         "A?????",
         "D?????",
         "E?????");
      noteInput.setShouldConsumeEvents(false);
      initCcPreferences(host);

      host.getMidiInPort(0).setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi);
      transport.isPlaying().addValueObserver((BooleanValueChangedCallback) this::onIsPlayingChanged);
      transport.isArrangerRecordEnabled().addValueObserver((BooleanValueChangedCallback) this::onRecordChanged);
      transport.isMetronomeEnabled().addValueObserver((BooleanValueChangedCallback) this::onMetronomeEnabledChanged);
      transport.preRoll().addValueObserver(
         (EnumValueChangedCallback) value -> onPreRollChanged((String) value));
      transport.playPosition().addValueObserver((DoubleValueChangedCallback) this::onPlayPositionChanged);
      transport.timeSignature().numerator().addValueObserver(
         (IntegerValueChangedCallback) this::onTimeSignatureNumeratorChanged);
      transport.timeSignature().denominator().addValueObserver(
         (IntegerValueChangedCallback) this::onTimeSignatureDenominatorChanged);
      syncStateToOmxOnInit();

      host.println("OMX-27 Initialized");
   }

   @Override
   public void exit()
   {
      ((ControllerHost) getHost()).println("OMX-27 Exited");
   }

   @Override
   public void flush()
   {
      // No MIDI feedback yet.
   }

   private void onMidi(final ShortMidiMessage msg)
   {
      if (!msg.isControlChange())
      {
         return;
      }

      final int cc = msg.getData1();
      final int value = msg.getData2();

      if (cc == stopCc && value == 127)
      {
         transport.stop();
         return;
      }

      if (cc == playCc)
      {
         transport.play();
         return;
      }

      if (cc == recordCc)
      {
         transport.record();
         return;
      }

      if (cc == metronomeToggleCc)
      {
         // Firmware may send 127 = on and 0 = off on the same CC; do not use toggle() or 0 is ignored
         // and Bitwig state diverges from the OMX LED.
         if (value == MIDI_ON)
         {
            transport.isMetronomeEnabled().set(true);
         }
         else if (value == MIDI_OFF)
         {
            transport.isMetronomeEnabled().set(false);
         }
         return;
      }

      if (cc == countInCc)
      {
         final String preRoll = countInCcValueToPreRoll(value);
         if (preRoll != null)
         {
            transport.preRoll().set(preRoll);
         }
         return;
      }

      if (cc == visibleMetronomeToggleCc)
      {
         if (value == MIDI_ON)
         {
            onVisibleMetronomeEnabledChanged(true);
         }
         else if (value == MIDI_OFF)
         {
            onVisibleMetronomeEnabledChanged(false);
         }
         return;
      }

      if (cc == send1Cc)
      {
         cursorTrack.getSend(0).set(value, MIDI_CC_RANGE);
         return;
      }

      if (cc == send2Cc)
      {
         cursorTrack.getSend(1).set(value, MIDI_CC_RANGE);
         return;
      }

      if (cc == send3Cc)
      {
         cursorTrack.getSend(2).set(value, MIDI_CC_RANGE);
         return;
      }

      if (cc == panCc)
      {
         cursorTrack.pan().set(value, MIDI_CC_RANGE);
         return;
      }

      final int remoteIndex = findRemoteKnobIndex(cc);
      if (remoteIndex >= 0)
      {
         remoteControlsPage.getParameter(remoteIndex).set(value, MIDI_CC_RANGE);
         return;
      }

      if (cc == volumeKnobCc)
      {
         cursorTrack.volume().set(value, MIDI_CC_RANGE);
      }
   }

   private void onIsPlayingChanged(final boolean isPlaying)
   {
      this.isPlaying = isPlaying;
      sendCc(playCc, isPlaying ? MIDI_ON : MIDI_OFF);

      if (!isPlaying)
      {
         stopVisualMetronome();
         pulseStop();
      }
      else
      {
         sendCc(stopCc, MIDI_OFF);
         if (isVisibleMetronomeEnabled)
         {
            startVisualMetronome();
         }
      }
   }

   private void onRecordChanged(final boolean isRecording)
   {
      sendCc(recordCc, isRecording ? MIDI_ON : MIDI_OFF);
   }

   private void onMetronomeEnabledChanged(final boolean isEnabled)
   {
      sendCc(metronomeToggleCc, isEnabled ? MIDI_ON : MIDI_OFF);
   }

   private void onVisibleMetronomeEnabledChanged(final boolean isEnabled)
   {
      isVisibleMetronomeEnabled = isEnabled;
      sendCc(visibleMetronomeToggleCc, isEnabled ? MIDI_ON : MIDI_OFF);

      if (isEnabled && isPlaying)
      {
         startVisualMetronome();
      }
      else
      {
         stopVisualMetronome();
      }
   }

   private void onPreRollChanged(final String preRoll)
   {
      sendCc(countInCc, preRollToCountInCcValue(preRoll));
   }

   private void onPlayPositionChanged(final double playPositionInQuarterNotes)
   {
      if (!isPlaying || !isVisibleMetronomeEnabled)
      {
         return;
      }

      final double beatLengthInQuarterNotes = 4.0 / Math.max(timeSignatureDenominator, 1);
      final long beatNumber = (long) Math.floor((playPositionInQuarterNotes / beatLengthInQuarterNotes) + 1e-9);
      if (beatNumber == lastPulseBeatNumber)
      {
         return;
      }

      lastPulseBeatNumber = beatNumber;
      final boolean isDownBeat = Math.floorMod(beatNumber, Math.max(timeSignatureNumerator, 1)) == 0;
      sendCc(metronomeFlashCc, isDownBeat ? MIDI_ON : METRONOME_FLASH_OFF_BEAT);
   }

   private void onTimeSignatureNumeratorChanged(final int numerator)
   {
      if (numerator > 0)
      {
         timeSignatureNumerator = numerator;
         lastPulseBeatNumber = Long.MIN_VALUE;
      }
   }

   private void onTimeSignatureDenominatorChanged(final int denominator)
   {
      if (denominator > 0)
      {
         timeSignatureDenominator = denominator;
      }
   }

   private void pulseStop()
   {
      sendCc(stopCc, MIDI_ON);
      ((ControllerHost) getHost()).scheduleTask(() -> sendCc(stopCc, MIDI_OFF), 60);
   }

   private void sendCc(final int cc, final int value)
   {
      if (midiOut != null)
      {
         midiOut.sendMidi(MIDI_CHANNEL_1_CC_STATUS, cc, value);
      }
   }

   private void syncStateToOmxOnInit()
   {
      // Keep firmware state deterministic on script load: visible metronome starts off.
      isVisibleMetronomeEnabled = false;
      sendCc(visibleMetronomeToggleCc, MIDI_OFF);
      sendCc(metronomeFlashCc, MIDI_OFF);

      // Push current host state immediately instead of waiting for observer callbacks.
      onIsPlayingChanged(transport.isPlaying().get());
      onRecordChanged(transport.isArrangerRecordEnabled().get());
      onMetronomeEnabledChanged(transport.isMetronomeEnabled().get());
      onPreRollChanged(transport.preRoll().get());
   }

   private void startVisualMetronome()
   {
      lastPulseBeatNumber = Long.MIN_VALUE;
   }

   private void stopVisualMetronome()
   {
      lastPulseBeatNumber = Long.MIN_VALUE;
      sendCc(metronomeFlashCc, MIDI_OFF);
   }

   private static String countInCcValueToPreRoll(final int value)
   {
      if (value == 0)
      {
         return "none";
      }
      if (value == 1)
      {
         return "one_bar";
      }
      if (value == 2)
      {
         return "two_bars";
      }
      if (value == 4)
      {
         return "four_bars";
      }
      return null;
   }

   private static int preRollToCountInCcValue(final String preRoll)
   {
      if ("one_bar".equals(preRoll))
      {
         return 1;
      }
      if ("two_bars".equals(preRoll))
      {
         return 2;
      }
      if ("four_bars".equals(preRoll))
      {
         return 4;
      }
      return 0;
   }

   private void initCcPreferences(final ControllerHost host)
   {
      final Preferences preferences = host.getPreferences();
      final String transportCategory = "Transport CC";
      final String knobCategory = "Knob CC";
      final String metronomeCategory = "Metronome CC";

      final SettableRangedValue stopCcSetting =
         preferences.getNumberSetting("Stop CC", transportCategory, 0, 127, 1, "", DEFAULT_CC_STOP);
      final SettableRangedValue playCcSetting =
         preferences.getNumberSetting("Play CC", transportCategory, 0, 127, 1, "", DEFAULT_CC_PLAY);
      final SettableRangedValue recordCcSetting =
         preferences.getNumberSetting("Record CC", transportCategory, 0, 127, 1, "", DEFAULT_CC_RECORD);
      final SettableRangedValue metronomeToggleCcSetting =
         preferences.getNumberSetting("Metronome Toggle CC", metronomeCategory, 0, 127, 1, "", DEFAULT_CC_METRONOME_TOGGLE);
      final SettableRangedValue countInCcSetting =
         preferences.getNumberSetting("Count-in CC", metronomeCategory, 0, 127, 1, "", DEFAULT_CC_COUNT_IN);
      final SettableRangedValue metronomeFlashCcSetting =
         preferences.getNumberSetting("Metronome Flash CC", metronomeCategory, 0, 127, 1, "", DEFAULT_CC_METRONOME_FLASH);
      final SettableRangedValue visibleMetronomeToggleCcSetting =
         preferences.getNumberSetting(
            "Visible Metronome Toggle CC",
            metronomeCategory,
            0,
            127,
            1,
            "",
            DEFAULT_CC_VISIBLE_METRONOME_TOGGLE);

      stopCcSetting.addValueObserver(MIDI_CC_RANGE, (IntegerValueChangedCallback) value -> stopCc = value);
      playCcSetting.addValueObserver(MIDI_CC_RANGE, (IntegerValueChangedCallback) value -> playCc = value);
      recordCcSetting.addValueObserver(MIDI_CC_RANGE, (IntegerValueChangedCallback) value -> recordCc = value);
      metronomeToggleCcSetting.addValueObserver(
         MIDI_CC_RANGE, (IntegerValueChangedCallback) value -> metronomeToggleCc = value);
      countInCcSetting.addValueObserver(MIDI_CC_RANGE, (IntegerValueChangedCallback) value -> countInCc = value);
      metronomeFlashCcSetting.addValueObserver(
         MIDI_CC_RANGE, (IntegerValueChangedCallback) value -> metronomeFlashCc = value);
      visibleMetronomeToggleCcSetting.addValueObserver(
         MIDI_CC_RANGE, (IntegerValueChangedCallback) value -> visibleMetronomeToggleCc = value);

      for (int i = 0; i < REMOTE_KNOB_COUNT; i++)
      {
         final int index = i;
         final int defaultCc = DEFAULT_REMOTE_KNOB_CCS[i];
         final SettableRangedValue remoteCcSetting = preferences.getNumberSetting(
            "Remote " + (i + 1) + " CC",
            knobCategory,
            0,
            127,
            1,
            "",
            defaultCc);
         remoteCcSetting.addValueObserver(MIDI_CC_RANGE,
            (IntegerValueChangedCallback) value -> remoteKnobCcs[index] = value);
      }

      final SettableRangedValue volumeCcSetting = preferences.getNumberSetting(
         "Volume CC",
         knobCategory,
         0,
         127,
         1,
         "",
         DEFAULT_VOLUME_KNOB_CC);
      volumeCcSetting.addValueObserver(MIDI_CC_RANGE, (IntegerValueChangedCallback) value -> volumeKnobCc = value);

      final SettableRangedValue panCcSetting = preferences.getNumberSetting(
         "Pan CC",
         knobCategory,
         0,
         127,
         1,
         "",
         DEFAULT_CC_PAN);
      final SettableRangedValue send1CcSetting = preferences.getNumberSetting(
         "Send 1 CC",
         knobCategory,
         0,
         127,
         1,
         "",
         DEFAULT_CC_SEND1);
      final SettableRangedValue send2CcSetting = preferences.getNumberSetting(
         "Send 2 CC",
         knobCategory,
         0,
         127,
         1,
         "",
         DEFAULT_CC_SEND2);
      final SettableRangedValue send3CcSetting = preferences.getNumberSetting(
         "Send 3 CC",
         knobCategory,
         0,
         127,
         1,
         "",
         DEFAULT_CC_SEND3);
      panCcSetting.addValueObserver(MIDI_CC_RANGE, (IntegerValueChangedCallback) value -> panCc = value);
      send1CcSetting.addValueObserver(MIDI_CC_RANGE, (IntegerValueChangedCallback) value -> send1Cc = value);
      send2CcSetting.addValueObserver(MIDI_CC_RANGE, (IntegerValueChangedCallback) value -> send2Cc = value);
      send3CcSetting.addValueObserver(MIDI_CC_RANGE, (IntegerValueChangedCallback) value -> send3Cc = value);
   }

   private int findRemoteKnobIndex(final int cc)
   {
      for (int i = 0; i < REMOTE_KNOB_COUNT; i++)
      {
         if (remoteKnobCcs[i] == cc)
         {
            return i;
         }
      }
      return -1;
   }
}
