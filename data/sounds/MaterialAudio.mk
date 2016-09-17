# Copyright 2016 Nitrogen Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Material Google Audio Files from Android Nougat
# Use material audio sounds from bullhead factory image
#

# PATHS
LOCAL_PATH := frameworks/base/data/sounds/material
OUT_SYS_PATH := system/media/audio

# Alarms
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/alarms/Argon.ogg:$(OUT_SYS_PATH)/alarms/Argon.ogg \
    $(LOCAL_PATH)/alarms/Awaken.ogg:$(OUT_SYS_PATH)/alarms/Awaken.ogg \
    $(LOCAL_PATH)/alarms/Bounce.ogg:$(OUT_SYS_PATH)/alarms/Bounce.ogg \
    $(LOCAL_PATH)/alarms/Carbon.ogg:$(OUT_SYS_PATH)/alarms/Carbon.ogg \
    $(LOCAL_PATH)/alarms/Drip.ogg:$(OUT_SYS_PATH)/alarms/Drip.ogg \
    $(LOCAL_PATH)/alarms/Gallop.ogg:$(OUT_SYS_PATH)/alarms/Gallop.ogg \
    $(LOCAL_PATH)/alarms/Helium.ogg:$(OUT_SYS_PATH)/alarms/Helium.ogg \
    $(LOCAL_PATH)/alarms/Krypton.ogg:$(OUT_SYS_PATH)/alarms/Krypton.ogg \
    $(LOCAL_PATH)/alarms/Neon.ogg:$(OUT_SYS_PATH)/alarms/Neon.ogg \
    $(LOCAL_PATH)/alarms/Nudge.ogg:$(OUT_SYS_PATH)/alarms/Nudge.ogg \
    $(LOCAL_PATH)/alarms/Orbit.ogg:$(OUT_SYS_PATH)/alarms/Orbit.ogg \
    $(LOCAL_PATH)/alarms/Osmium.ogg:$(OUT_SYS_PATH)/alarms/Osmium.ogg \
    $(LOCAL_PATH)/alarms/Oxygen.ogg:$(OUT_SYS_PATH)/alarms/Oxygen.ogg \
    $(LOCAL_PATH)/alarms/Platinum.ogg:$(OUT_SYS_PATH)/alarms/Platinum.ogg \
    $(LOCAL_PATH)/alarms/Rise.ogg:$(OUT_SYS_PATH)/alarms/Rise.ogg \
    $(LOCAL_PATH)/alarms/Sway.ogg:$(OUT_SYS_PATH)/alarms/Sway.ogg \
    $(LOCAL_PATH)/alarms/Timer.ogg:$(OUT_SYS_PATH)/alarms/Timer.ogg

# Notifications
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/notifications/Ariel.ogg:$(OUT_SYS_PATH)/notifications/Ariel.ogg \
    $(LOCAL_PATH)/notifications/Carme.ogg:$(OUT_SYS_PATH)/notifications/Carme.ogg \
    $(LOCAL_PATH)/notifications/Ceres.ogg:$(OUT_SYS_PATH)/notifications/Ceres.ogg \
    $(LOCAL_PATH)/notifications/Elara.ogg:$(OUT_SYS_PATH)/notifications/Elara.ogg \
    $(LOCAL_PATH)/notifications/Europa.ogg:$(OUT_SYS_PATH)/notifications/Europa.ogg \
    $(LOCAL_PATH)/notifications/Iapetus.ogg:$(OUT_SYS_PATH)/notifications/Iapetus.ogg \
    $(LOCAL_PATH)/notifications/Io.ogg:$(OUT_SYS_PATH)/notifications/Io.ogg \
    $(LOCAL_PATH)/notifications/Rhea.ogg:$(OUT_SYS_PATH)/notifications/Rhea.ogg \
    $(LOCAL_PATH)/notifications/Salacia.ogg:$(OUT_SYS_PATH)/notifications/Salacia.ogg \
    $(LOCAL_PATH)/notifications/Tethys.ogg:$(OUT_SYS_PATH)/notifications/Tethys.ogg \
    $(LOCAL_PATH)/notifications/Titan.ogg:$(OUT_SYS_PATH)/notifications/Titan.ogg

# Ringtones
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/ringtones/Atria.ogg:$(OUT_SYS_PATH)/ringtones/Atria.ogg \
    $(LOCAL_PATH)/ringtones/Callisto.ogg:$(OUT_SYS_PATH)/ringtones/Callisto.ogg \
    $(LOCAL_PATH)/ringtones/Dione.ogg:$(OUT_SYS_PATH)/ringtones/Dione.ogg \
    $(LOCAL_PATH)/ringtones/Ganymede.ogg:$(OUT_SYS_PATH)/ringtones/Ganymede.ogg \
    $(LOCAL_PATH)/ringtones/Luna.ogg:$(OUT_SYS_PATH)/ringtones/Luna.ogg \
    $(LOCAL_PATH)/ringtones/Oberon.ogg:$(OUT_SYS_PATH)/ringtones/Oberon.ogg \
    $(LOCAL_PATH)/ringtones/Phobos.ogg:$(OUT_SYS_PATH)/ringtones/Phobos.ogg \
    $(LOCAL_PATH)/ringtones/Pyxis.ogg:$(OUT_SYS_PATH)/ringtones/Pyxis.ogg \
    $(LOCAL_PATH)/ringtones/Sedna.ogg:$(OUT_SYS_PATH)/ringtones/Sedna.ogg \
    $(LOCAL_PATH)/ringtones/Titania.ogg:$(OUT_SYS_PATH)/ringtones/Titania.ogg \
    $(LOCAL_PATH)/ringtones/Triton.ogg:$(OUT_SYS_PATH)/ringtones/Triton.ogg \
    $(LOCAL_PATH)/ringtones/Umbriel.ogg:$(OUT_SYS_PATH)/ringtones/Umbriel.ogg

# UI
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/ui/audio_end.ogg:$(OUT_SYS_PATH)/ui/audio_end.ogg \
    $(LOCAL_PATH)/ui/audio_initiate.ogg:$(OUT_SYS_PATH)/ui/audio_initiate.ogg \
    $(LOCAL_PATH)/ui/camera_click.ogg:$(OUT_SYS_PATH)/ui/camera_click.ogg \
    $(LOCAL_PATH)/ui/camera_focus.ogg:$(OUT_SYS_PATH)/ui/camera_focus.ogg \
    $(LOCAL_PATH)/ui/Dock.ogg:$(OUT_SYS_PATH)/ui/Dock.ogg \
    $(LOCAL_PATH)/ui/Effect_Tick.ogg:$(OUT_SYS_PATH)/ui/Effect_Tick.ogg \
    $(LOCAL_PATH)/ui/KeypressDelete.ogg:$(OUT_SYS_PATH)/ui/KeypressDelete.ogg \
    $(LOCAL_PATH)/ui/KeypressInvalid.ogg:$(OUT_SYS_PATH)/ui/KeypressInvalid.ogg \
    $(LOCAL_PATH)/ui/KeypressReturn.ogg:$(OUT_SYS_PATH)/ui/KeypressReturn.ogg \
    $(LOCAL_PATH)/ui/KeypressSpacebar.ogg:$(OUT_SYS_PATH)/ui/KeypressSpacebar.ogg \
    $(LOCAL_PATH)/ui/KeypressStandard.ogg:$(OUT_SYS_PATH)/ui/KeypressStandard.ogg \
    $(LOCAL_PATH)/ui/Lock.ogg:$(OUT_SYS_PATH)/ui/Lock.ogg \
    $(LOCAL_PATH)/ui/LowBattery.ogg:$(OUT_SYS_PATH)/ui/LowBattery.ogg \
    $(LOCAL_PATH)/ui/NFCFailure.ogg:$(OUT_SYS_PATH)/ui/NFCFailure.ogg \
    $(LOCAL_PATH)/ui/NFCInitiated.ogg:$(OUT_SYS_PATH)/ui/NFCInitiated.ogg \
    $(LOCAL_PATH)/ui/NFCSuccess.ogg:$(OUT_SYS_PATH)/ui/NFCSuccess.ogg \
    $(LOCAL_PATH)/ui/NFCTransferComplete.ogg:$(OUT_SYS_PATH)/ui/NFCTransferComplete.ogg \
    $(LOCAL_PATH)/ui/NFCTransferInitiated.ogg:$(OUT_SYS_PATH)/ui/NFCTransferInitiated.ogg \
    $(LOCAL_PATH)/ui/Trusted.ogg:$(OUT_SYS_PATH)/ui/Trusted.ogg \
    $(LOCAL_PATH)/ui/Undock.ogg:$(OUT_SYS_PATH)/ui/Undock.ogg \
    $(LOCAL_PATH)/ui/Unlock.ogg:$(OUT_SYS_PATH)/ui/Unlock.ogg \
    $(LOCAL_PATH)/ui/VideoRecord.ogg:$(OUT_SYS_PATH)/ui/VideoRecord.ogg \
    $(LOCAL_PATH)/ui/VideoStop.ogg:$(OUT_SYS_PATH)/ui/VideoStop.ogg \
    $(LOCAL_PATH)/ui/WirelessChargingStarted.ogg:$(OUT_SYS_PATH)/ui/WirelessChargingStarted.ogg

