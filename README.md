# Gulag Exhibition Control System (GECS)

<img src="https://i.imgur.com/W8wbb4B.jpg" height="450">

## What is GECS?
Exhibition and any other multimedia equipment, especially made without the participation of big capital, often fails. In-house made interactive exhibits, projectors and kiosks have to be switched on and off manually, by technical personnel. That is neither convenient nor comfortable. 
GECS allows you to control and monitor the exhibition equipment. At the moment, these are the following devices:

- Windows PC
- projectors with PJLink support
- Sharp TVs
- Dataton Watchpax
- Raspberry PI on Raspbian or Raspberry PI OS
- presence or motion sensors

GECS can:
- turn on/off the devices together and/or separately
- monitor their status (working or broken)
- automatically restart if broken
- provide attenders with the opportunity to restart them through the Telegram bot.

Of course, there are commercial solutions that can do all this (for example, Crestron). But it is not always possible to pay that much money for the required hardware and software. And, athough flexible and ritch, commercial control systems are demanding in terms of specialized languages and fsdaf 
Of course, there are open-source solutions that can do all this (for example, OpenHab). But in order to set them up, you need to learn the OpenHab language. And, when things go wrong, Java. We have decided that it is better to use Java right away.
## How to install GECS?
GECS is a program written in JAVA 8 SE. At the moment, it has only been tested on Windows 10. To run it, you need to:
1. Install [Java](https://www.java.com/ru/) if you don't have it installed yet
2. Download [GECS program]
3. Unpack the program and run it by writing on the command line
```
java -jar GECS.jar
```
4. It will start and display the hardware configuration of the Gulag History Museum. You probably don't need it. In order for you to be able to do what you need to do, you need to write your hardware configuration into JSON configuration files
## Set up JSON
All GECS settings are stored in JSON files:
structure.json - information about devices and their configuration
cronSchedule.json - information about the timetable of switching on and off
specialSchedule.json - information about exclusions from the timetable 
views.json - information on how to place the device icons in the GECS window
viewTypes.json - information about how devices icons look in the GECS window
sources.json - information about external command sources
bots.json - information about telegram bots
### Configuring exhibition in structure.json
The structure in which the devices reside

<img src="https://i.imgur.com/AWEZy5P.jpg" height="250">

#### Structural units

**switchGroup** - switching group - a set of devices and modules that the system turns on and off simultaneously and in parallel. Turning on and off can be scheduled according to the schedule in the cronSchedule.json and specialSchedule.json files. The group is displayed in the interface as a tab. Turning on, turning off and checking the current status of all devices in the group is done using the Switch all off, Switch all on and Update buttons.

As a rule, a group is used for functionally different rooms. For example, the group "exposition", "cafe", "library".

**vismodule** - display module. Includes one *signal output device* and many *display devices*. If the display module is in the inclusion group, then the devices are turned on according to the following algorithm:
1. All display devices turn on at the same time. If at least one display device fails, switching on is stopped.
2. The signal output device is turned on.

Shutdown occurs in reverse order.

The display module is controlled by an automatic reboot and shutdown system (Watchdog).

If a signal emitter does not have a controlled restart command and it fails to turn on, Watchdog will automatically turn off all display devices and report an error to the console and Exposure Bot.

If the alert device has a controlled restart command (currently it is PCWithDaemon, VLCPlayer, VLCPlayer2Screen), then Watchdog works according to the following principle. If a failure occurs during power-up or regular check (check) of the alarm device, Watchdog will issue a restart command. If it fails again after a restart, Watchdog will turn off all display devices and report an error to the console and Exposure Bot.

The display module is not displayed in the interface.

**module** - general purpose module. When turned on and off, the module sequentially turns on / off the specified devices. If one of the devices fails during switching on/off, switching on/off is stopped. Switching on and off occurs in the reverse order.

The general purpose module is not displayed in the interface.

**device** - device. To be in an enable group, a device must be able to turn on and off. The device is displayed in the interface according to the settings in the views.json file.

#### structrue.json - attributes of structural units
JSON consists of 5 sections:

- `devices`
-`vismodules`
- `modules`
- `switchGroups`

Each section includes structural units, which are described by a set of attributes. For example `device`

```
    {
      "name": "PC1-1",
      "ip": "192.168.0.1",
      "mac": "FC-AA-14-CC-6B-91",
      "description": "Propaganda, First camps",
      "factory": "VLCPlayer2ScreenFactory"
    }
```
`name` is the unique name of the structural unit. Present in all structural units.


**`device`** - describes the device type. The attributes are specific to the specific device type specified in the `factory` parameter:

Windows PC devices:

- `PCWithDaemonFactory` - A Windows PC with a Daemon resident program running on it.
- `VLCPlayerFactory` - Windows PC with VLCPlayer program installed on it.
- `VLCPlayer2ScreenFactory` - Windows PC with two VLCPlayer programs installed on it. Programs must differ by port.

For Windows PC devices, you must specify `mac` - the mac address of the computer

Other devices:

- `ProjectorFactory` - any projector that supports PJLink
- `SharpTVFactory` - Sharp Aquos series TV
- `RaspberryFactory` - Raspberry PI with running resident Daemon

All of these devices are:
`ip` — device ip address
`description` is a description of the device.

**`vismodule`** describes the attributes of the visualization module.
`visualisers` - list of display devices
`source` is the signal issuing device.

**`module`** describes the attributes of a general purpose module.
`sequence` - the sequence in which devices are turned on or off.

**`switchGroup`** describes the attributes of an include group.
`list` is a list of simultaneously enabled devices. This may include `device`, `vismodule` and `module`

#### cronSchedule.json - parameters for automatic regular scheduled on/off in
JSON consists of a list of inclusion rules applied to the inclusion groups

An example of an include rule that sends an enable command to the `Expo` enable group every day at 11:15 AM and an OFF command at 21:00 PM. The rules are set in the [crontrigger] language (http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html)

```
{
"switchGroup": "Expo",
"switch on": {
    "include": [
      "0 15 11 ? * *"
    ],
    "exclude": [
    ]
  },
"switch off": {
    "include": [
      "0 0 21 ? ​​* *"
    ],
    "exclude": [
    ]
  }
}
```
The entered values ​​are displayed in the interface in the tab of the corresponding inclusion group.

#### specialSchedule.json - parameters for automatic on/off on special days
The JSON consists of a list of special inclusion conditions on a specific day that apply to inclusion groups. This schedule takes precedence over cronSchedule and is used to set special hours to turn on and off on special days. For example, on the fifth of January on Friday, the exposure must be turned on earlier - at 9:45, and turned off later - at 21:30.

```
{
  "switchGroup" : "Expo",
  "date" : "5 1 2019",
  "switch on" : "0 45 09",
  "switch off" : "0 30 21"
}
```
Conditions can be set using the interface.

<img src="https://i.imgur.com/0MflBpG.jpg" height="200">

You must select the date, time to turn on and off and press the Set button. The new entry will appear in specialSchedule.json

#### views.json - parameters for displaying devices and inclusion groups on the interface
JSON contains two sections:
- `devices` contains device mapping information
- `switchGroups` contains information about switch groups

The device mapping information includes a unique device name `name` corresponding to the name from structure.json. As well as icon coordinates `x-icon`, `y-icon`, rotation angle in degrees `rot`, label coordinates `x-label`, `y-label` and the type of icon in the `type` attribute. Icon types are defined in the viewTypes.json file.

```
{
  "name": "PC1-1",
  "x-icon": 77,
  "y-icon": 66,
  "rot": 0,
  "x-label": 77,
  "y-label": 85,
  "type": "PC"
}
```

Information about inclusion groups contains the name of the group from structure.json, the position in the bookmarks on the interface `position` and the background image `image`.

```
{
  "name": "expo",
  position: 0
  "image": "capture.jpg"
}
```

#### viewTypes.json - parameters for displaying device types on the interface
JSON contains information about device icons displayed in the interface. Each data set contains the name of the type `type`, a list of coordinates, according to which the polygon of the `polygon` icon will be built, and `letter` - the letter displayed in the center of the polygon.
```
{
    "type":"PC",
    "polygon":[
        "0.0",
        "0.0",
        "20.0",
        "0.0",
        "20.0",
        "20.0",
        "0.0",
        "20.0"
    ],
    "letter":"B"
}
```

#### sources.json - external event handler parameters
External event handlers read data coming as external TCP/IP messages from port 11213 and activate the command. The message must be sent in the format `<handler name>:<signal name>`.
JSON contains a list of handlers. Processing Information
The parent includes the handler name `name`, and a list of input signals `signals`. Each input signal contains a name `name` and `actions` a list of commands to be executed in sequence when the signal is activated. The commands in the list include `device` - a structural unit (any, not just a device) that will execute the command and `command` - the textual name of the command. The name of the command can be found by right-clicking on the object in the interface.

```
  {
    "name": "starter",
    "signals": [
      {
        "name": "on",
        "actions": [
          {
            "device": "Expo",
            "command": "switchOn"
          }
        ]
      },
      {
        "name": "off",
        "actions": [
          {
            "device": "Expo",
            "command": "switch Off"
          }
        ]
      }
    ]
  }
```

#### bots.json - service bot parameters
The JSON includes information that allows you to connect a Telegram bot, with which users of the group can control the exposure. In order to connect a bot, you need to create it and fix its token (XXX) and name (NNNNNN). Then you need to create a group and connect this bot there. You need to find the code of the created group. To do this, you need to go to the group through the web interface and fix in the address bar https://web.telegram.org/#/im?p=gYYYYYYYYY where instead of YYYYYYYYY there should be a number - the code of the created group.

The data includes the name of the bot `name`, its token `token`, codes of chats in which the bot is available `allowedChats` (the code must be preceded by a minus sign). The proxy port `proxyPort` is also set. Proxy parameters are hardcoded in the program.

The work of the bot is determined by the bot's responses to user requests `responces`. The response action specifies the action type `type`, the text request `request` in response to which the response action is activated, and the text that accompanies the response action.

```
"name":"NNNNNN",
"token":"XXXXXXXXX:XXXXXXXXXXXXXXXXXXXXXXXX",
"allowedChats":[-YYYYYYYYY],
"proxyPort":9150,
```

There are three types of responses:

- `choice` demonstrates the interface of the choices specified in the list

```
{
    "type":"choice",
    "request":"Bot",
    "text":" please choose what to do:",
    "choice":[
      "turn on exposure",
      "turn off exposure"
    ]
}
```

- `action` activates the `command` command on the `device` structural unit (this can be not only a device, but a module or a group)

```
{
    "type":"action",
    "request":"enable exposure",
    "device":"Expo",
    "command":"switchOff"
}
```

- `text` displays response text

```
{
    "type":"text",
    "request":"do nothing",
    "text":"That's nice"
}
```
# Gulag Exhibition Control System (GECS)

<img src="https://i.imgur.com/W8wbb4B.jpg" height="450">

## Что такое GECS?
Музейная техника, особенно сделанная без участия крупного капитала часто подводит. Ее не удобно обслуживать и следить за ней. Но как правило ей можно удаленно управлять.
GECS позволяет управлять экспозционной техникой. На данный момент это следующие устройства:

- Windows PC
- проекторы с поодержкой PJLink
- телевизоры Sharp
- Dataton Watchpax
- Raspberry PI на Raspbian
- сенсоры присутсвия или движения

GECS умеет:
- включать/выключать устройства вместе и по отдельности
- наблюдать за их состоянием (работает-сломалось)
- автоматически перезагружать если сломалось
- предоставлять смотрителям возможность чинить если они видят что что-то сломалось через Telegram bot. 

Разумеется, есть коммерческие решения, которые это все умеют (например Crestron). Но, во-первых, не всегда есть возможность платить деньги, а во-вторых, хочется иметь управление и возможность настройки в руках. 
Разумеется, есть open-source, который это все умеет (например OpenHab). Но для того, чтобы его качественно и полноценно его настроить нужно выучить язык OpenHab, а когда дела идут плохо, Java. Мы решили, что лучше сразу использовать Java.
## Как установить GECS?
GECS - программа, написанная на JAVA 8 SE. На данный момент она испытана только на Windows 10. Чтобы запустить ее нужно:
1. Установить [Java](https://www.java.com/ru/) если у вас он еще не установлен
2. Скачать [программу GECS]()
3. Распаковать программу и запустить ее, написав в командной строке
```
java -jar GECS.jar
```
4. Она запустится и отобразит конфигурацию оборудования Музея истории ГУЛАГа. Вряд ли вам это нужно. Для того, чтобы вы могли сделать то, что вам нужно необходимо записать вашу конфигурацию оборудования в настроечные файлы JSON
## Настраиваем JSON
Все настройки GECS хранятся в JSON файлах:
structure.json - информация об устройствах и их конфигурация
cronSchedule.json - информация о регулярном включении и выключении экспозиции
specialSchedule.json - информация о специальных днях включения и выключения экспозиции
views.json - информация о том, как отображать устройства в окне GECS
viewTypes.json - информация о том, как выглядят устройства в окне GECS
sources.json - информация о внешних источниках команд
bots.json - информация о телеграм-ботах
### Конфигурируем экспозицию в structure.json
Структура, в которой находятся устройства

<img src="https://i.imgur.com/AWEZy5P.jpg" height="250">

#### Структурные единицы

**switchGroup** - группа включения - множество устройств и модулей, которые система включает и выключает одновременно и параллельно. Включение и выключение  можно задать по расписанию в файлах cronSchedule.json и specialSchedule.json. Группа отображается в интерфейсе в виде закладки. Включение, выключение и проверка текущего состояния всех устройств группы производится по кнопкам Switch all off, Switch all on и Update.

Как правило, группа испозльзуется для функционально разных помещений. Например, группа "экспозиция", "кафе", "библиотека".

**vismodule** - модуль отображения. Включает в себя одно *устройство выдачи сигнала* и множество *устройств отображения*. Если модуль отображения находится в группе включения, то включение устройств происходит по следующему алгоритму:
1. Одновременно включаются все устройства отображения. Если хотя бы одно устройство отбражения дол сбой, включение останавливается.
2. Включается устройство выдачи сигнала.

Выключение происходит в обратном порядке.

Модуль отображения контролируется автоматической системой перезагрузки и отключения (Watchdog). 

Если устройство выдачи сигнала не имеет имеет команды контролируемого перезапуска, и при его включении произошел сбой, Watchdog автоматически выключит все устройства отображения и выдаст ошибку на консоль и на Бот экспозиции.

Если устройство выдачи сигнала имеет команду контролируемого перезапуска (сейчас это PCWithDaemon, VLCPlayer, VLCPlayer2Screen), то Watchdog работает по следующему принципу. Если при включении или регулярной проверке (check) устройства выдачи сигнала произошел сбой, Watchdog подаст команду на перезапуск. Если после перезапуска опять произошел сбой, Watchdog выключит все устройства отображения и выдаст ошибку на консоль и на Бот экспозиции. 

Модуль отображения не отображается в интерфейсе.

**module** - модуль общего назначения. При включении и выключении модуль последовательно включает/выключает указанные устройства. Если в ходе включения/выключения одно из устройств дало сбой, включение/выключение прекращаеся. Включение и выключения происходят в обратном порядке.

Модуль общего назначения не отображается в интерфейсе.

**device** - устройство. Для того, чтобы находться в группе включения, устройство должно иметь возможность включаться и выключаться. Устройство отображается в интерейсе в соответствии с настройками в файле views.json.

#### structrue.json — атрибуты структурных единиц
JSON состоит из 5 разделов:

- `devices`
- `vismodules`
- `modules`
- `switchGroups`

Каждый раздел включает в себя структурные единицы, которые описываются набором атрибутов. Например `device`

```
    {
      "name": "PC1-1",
      "ip": "192.168.0.1",
      "mac": "FC-AA-14-CC-6B-91",
      "description": "Пропаганда, Первые лагеря",
      "factory": "VLCPlayer2ScreenFactory"
    }
```
`name` — уникальное наименование структурной единицы. Присутствует у всех структурных единиц.


**`device`** — описывает тип устройства. Атрибуты специфичны конкретному типу устройства, указываемому в параметре `factory`:

Устройства Windows PC:

- `PCWithDaemonFactory` — Windows PC с запущенным на нем резидентной программой Daemon. 
- `VLCPlayerFactory` —  Windows PC с запкщенным на нем программой VLCPlayer. 
- `VLCPlayer2ScreenFactory` —  Windows PC с запкщенным на нем двумя программами VLCPlayer. Программы должны отличаться портом.

Для устройств Windows PC необходимо указать `mac` — mac-адрес компьтера

Другие устройства:

- `ProjectorFactory` — любой проектор, поддерживающий PJLink
- `SharpTVFactory` — телевизор серии Sharp Aquos
- `RaspberryFactory` — Raspberry PI с работающей резидентной программой Daemon

Для всех этих устройств указыватся:
`ip` — ip-адрес устройства
`description` — описание устройства.

**`vismodule`** описывает атрибуты модуля визуализации. 
`visualisers` — перечень устройств отображения  
`source` — устройство выдачи сигнала.

**`module`** описывает атрибуты модуля общего назначения. 
`sequence` — последовательность в которой включаются или выключаются устройства. 

**`switchGroup`** описывает атрибуты группы включения. 
`list` — список одновременно включаемых устройств. Сюда могут входить `device`, `vismodule` и `module`

#### cronSchedule.json — параметры автоматического регулярного включения/выключения по расписанию в
JSON состоит из списка правил включения, применяемых к группам включения

Пример правила включения, подающего включение на группу включения `Expo` каждый день в 11:15 команду включения и в 21:00 команду выключения. Правила задаются на языке [crontrigger](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html)

```
{
"switchGroup": "Expo",
"switch on": {
    "include": [
      "0 15 11 ? * *"
    ],
    "exclude": [
    ]
  },
"switch off": {
    "include": [
      "0 0 21 ? * *"
    ],
    "exclude": [
    ]
  }
}
```
Введенные значения отображаются в интерфейсе в закладке соответствующей группы включения.

#### specialSchedule.json — параметры автоматического включения/выключения в специальные дни
JSON состоит из списка специальных условий включения в определенный день, применяемых к группам включения. Это расписание обладает большим приоритетом, чем cronSchedule и используется для задания специальных часов включения и выключения в особые дни. Например пятого января в пятницу экспозицию необходимо включить раньше - в 9:45, а выключить позже - в 21:30. 

```
{
  "switchGroup" : "Expo",
  "date" : "5 1 2019",
  "switch on" : "0 45 09",
  "switch off" : "0 30 21"
}
```
Условия можно задавать с помощью интерфейса. 

<img src="https://i.imgur.com/0MflBpG.jpg" height="200">

Необходимо выбрать дату, время включения и выключения и нажать кнопку Set. Новая запись появится в specialSchedule.json

#### views.json — параметры отображения устройств и групп включения на интерфейсе
JSON содержит два раздела:
- `devices` содержит информацию об отображении устройств
- `switchGroups` содержит информацию о группах включения

Информация об отображении устройств включает уникальное наименование устройства `name`, соответствущее имени из structure.json. А также координаты иконки `x-icon`,`y-icon`, угол поворота в градусах `rot`, координаты подписи `x-label`, `y-label` и вид иконки в атрибуте `type`. Виды иконок задаются в файле viewTypes.json. 

```
{
  "name": "PC1-1",
  "x-icon": 77,
  "y-icon": 66,
  "rot": 0,
  "x-label": 77,
  "y-label": 85,
  "type": "PC"
}
```

Информация о группах включения содержит наименование группы из structure.json, положение в закладках на интерфейсе `position` и фоновую картинку `image`.

```
{
  "name": "Expo",
  "position": 0,
  "image": "Capture.jpg"
}
```

#### viewTypes.json — параметры отображения типов устройств на интерфейсе
JSON содержит информацию о иконках устройств, отображаемых в интерфейсе. Каждый набор данных содержит наименование типа `type`, перечень координат, по которым будет построен многоугольник иконки `polygon` и `letter` - буква, отображаемая по центру многоугольника.
```
{
    "type":"PC",
    "polygon":[
        "0.0",
        "0.0",
        "20.0",
        "0.0",
        "20.0",
        "20.0",
        "0.0",
        "20.0"
    ],
    "letter":"B"
}
```

#### sources.json — параметры обработчика внешних событий 
Обработчики внешних событий считывают данные, поступающие в виде внешних TCP/IP сообщений с порта 11213 и активируют команду. Сообщение должно посылаться в формате `<имя обработчика>:<имя сигнала>`.
JSON содержит перечень обработчиков. Информация об обработчике включает в себя имя обработчика `name`, и перечень входных сигналов `signals`. Каждый входной сигнал содержит имя `name` и `actions` - список команд, последовательно исполняемых при активации сигнала. Команды в списке включают в себя `device` - структурная единица (любая, не только устройство), которая будет исполнять команду и `command` - текстовое наименование команды. Наименование команды можно узнать, нажав правой кнопкой на объект в интерфейсе.

```
  {
    "name": "starter",
    "signals": [
      {
        "name": "on",
        "actions": [
          {
            "device": "Expo",
            "command": "switchOn"
          }
        ]
      },
      {
        "name": "off",
        "actions": [
          {
            "device": "Expo",
            "command": "switchOff"
          }
        ]
      }
    ]
  }
```

#### bots.json — параметры бота обслуживания
JSON включает информацию, позволяющую подключить Telegram бот, с помощью которого пользователи группы могут контролировать экспозицию. Для того чтобы подключить бота его необходимо создать и зафиксировать его token (XXX) и имя (NNNNNNN). Затем необходимо создать группу и подключить туда этого бота. Необходимо найти код созданной группы. Для этого нужно зайти в группу через веб интерфейс и зафиксировать в адресной строке https://web.telegram.org/#/im?p=gYYYYYYYYY где вместо YYYYYYYYY должно быть число - код созданной группы.

Данные включают в себя имя бота `name`, его токен `token`, коды чатов, в которых бот доступен `allowedChats` (перед кодом необходимо поставить знак минуса). Также задается порт прокси `proxyPort`. Параметы прокси заданы жестко в программе.

Работа бота определяются ответными действиями бота на запросы пользователя `responces`. В ответном действии задается тип действия `type`, текстовый запрос `request` в ответ на который акивируется ответное действие и текст, которым сопровождается ответное действие.

```
"name":"NNNNNN",
"token":"XXXXXXXXX:XXXXXXXXXXXXXXXXXXXXXXXX",
"allowedChats":[-YYYYYYYYY],
"proxyPort":9150,
```

Ответные действия бывают трех типов:

- `choice` демонстрирует интерфейс выбора, указанных в списке

```
{
    "type":"choice",
    "request":"Бот",
    "text":" пожалуйста выберите что делать:",
    "choice":[
      "включить экспозицию",
      "выключить экспозицию"
    ]
}
```

- `action` активирует команду `command` на структурной единице `device` (это может быть не только устройство, но модуль или группа)

```
{
    "type":"action",
    "request":"включить экспозицию",
    "device":"Expo",
    "command":"switchOff"
}
```

- `text` выводит ответный текст

```
{
    "type":"text",
    "request":"ничего не чинить",
    "text":"вот и славно"
}
```
