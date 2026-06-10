# Mimir Android App

Графический интерфейс для библиотеки mimir-master. Kotlin + Jetpack Compose.

## Структура проекта

```
app/src/main/java/com/mimir/app/
├── MimirApplication.kt          — Application класс, инициализация БД
├── MainActivity.kt              — точка входа, навигация
├── data/
│   ├── Database.kt              — Room БД: Contact, Message, DAO
│   └── MimirBridge.kt           — обёртка над PeerNode (libmimir.so)
├── service/
│   ├── ConnectionService.kt     — foreground service, уведомления
│   └── BootReceiver.kt          — автозапуск после перезагрузки
├── ui/
│   ├── theme/
│   │   ├── Theme.kt             — Material3 тёмная/светлая тема
│   │   └── Typography.kt        — шрифты
│   ├── components/
│   │   ├── AvatarCircle.kt      — аватар с инициалами
│   │   └── AddContactDialog.kt  — диалог добавления контакта
│   └── screens/
│       ├── ContactListScreen.kt — список чатов
│       ├── ChatScreen.kt        — экран переписки
│       ├── ChatViewModel.kt     — логика чата
│       ├── ContactsViewModel.kt — логика списка контактов
│       ├── CallScreen.kt        — экран звонка
│       └── CallViewModel.kt     — логика звонка + AudioRecord/AudioTrack
```

## Подключение libmimir.so

1. Собрать нативную библиотеку из mimir-master:
   ```powershell
   cd mimir-master
   .\scripts\build_android.ps1
   ```

2. Скопировать результаты в проект:
   ```
   mimir-master/jniLibs/  →  app/jniLibs/
       arm64-v8a/libmimir.so
       armeabi-v7a/libmimir.so
       x86/libmimir.so
       x86_64/libmimir.so
   ```

3. Скопировать Kotlin биндинги:
   ```
   mimir-master/kotlin-bindings/uniffi/mimir/mimir.kt
       →  app/src/main/java/uniffi/mimir/mimir.kt
   ```

## Сборка

```bash
./gradlew assembleDebug
```

## Что реализовано

- **Список чатов** — контакты с онлайн-статусом, счётчик непрочитанных
- **Переписка** — текст, файлы с прогрессом отправки/получения, галочки доставки
- **Звонки** — входящие/исходящие, аудио через AudioRecord/AudioTrack (AAC 44100)
- **Уведомления** — сообщения с IMPORTANCE_HIGH (звук + всплытие), звонки с fullscreen intent
- **Фоновая работа** — foreground service + автозапуск после перезагрузки
- **Тема** — Material3, тёмная/светлая, изумрудно-синяя палитра

## Настройки Yggdrasil-пиров

Список bootstrap-пиров задаётся в `ConnectionService.kt`:
```kotlin
yggPeers = listOf(
    "tcp://de1.mimir.im:7743",
    "tcp://de2.mimir.im:7743",
    "tcp://sk1.mimir.im:7743",
)
```
