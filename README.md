————————

📌 Thesis: Bypass Charging Control for Oplus Devices

✏ Abstract
This project explores the concept and implementation of bypass charging on Oplus devices, a technique where the device powers itself directly from the charger, bypassing the battery during charging. This reduces battery wear and heat generation during prolonged or heavy charging sessions, enhancing battery lifespan and device thermal management.

✏ Introduction
Oplus smartphones employ advanced charging architectures involving power path controllers and smart charging ICs. In bypass charging mode, electrical power from the charger supplies the system directly instead of routing through the battery. This separation helps optimize device heat and battery performance, especially under demanding conditions such as gaming or 5G data use.

✏ Technical Background
⦁ Overview of Oplus charging ICs and power path controllers.
⦁ Description of battery charging versus bypass charging modes.
⦁ Role of kernel drivers and firmware in managing charging behavior.

✏ Design and Implementation
⦁ Investigating kernel interfaces and sysfs entries available on Oplus devices for charging control.
⦁ Reverse engineering proprietary charging modules and thermal management components.
⦁ Developing kernel patches or user-space scripts to enable conditional bypass charging (e.g., triggered by temperature or CPU load).
⦁ Monitoring battery health, thermal data, and system stability to evaluate impact.

✏ Benefits of Bypass Charging
⦁ Reduced Thermal Stress: Limits heat generation by preventing battery charge/discharge cycles during heavy usage.
⦁ Extended Battery Lifespan: Minimizes battery degradation by reducing charge cycle count.
⦁ Improved Performance: Maintains system stability and prevents thermal throttling during intensive scenarios like gaming or 5G streaming.
⦁ Enhanced User Experience: Enables faster, safer charging while the device is in heavy use.

✏ Challenges
⦁ Scarce public documentation on Oplus proprietary charging hardware and firmware.
⦁ Ensuring bypass charging activates only under safe conditions without risking battery or device safety.
⦁ Supporting multiple device models with varying hardware designs and software stacks.

✏ Conclusion
Bypass charging presents a viable approach to optimize thermal management and battery health on Oplus devices during intensive charging and usage periods. This project aims to provide insights and tools that help developers and enthusiasts explore and implement bypass charging control effectively.

————————

✏ How to Use

1. Download and install the bypass charging app for your Oplus device.
2. Grant Superuser (root) permission to the app when prompted.
3. Pull down your notification shade or Quick Settings (QS) panel.
4. Add the new tile named “Bypass Charging” to your active QS tiles area.
5. The QS tile will let you toggle bypass charging on or off easily.
6. Note: The tile only works when the charger is connected—once unplugged, bypass charging stops automatically to protect the battery.

————————

✏ Credits
⦁ Original concept and script by Thundergod Thor⚡
⦁ Application design and app development by Robinop

————————
