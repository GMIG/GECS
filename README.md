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
