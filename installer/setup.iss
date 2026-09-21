; Script de Inno Setup para DataBase Studio
; Genera un instalador nativo paso a paso con asistente en espanol, accesos directos y desinstalador.

#define MyAppName "DataBase Studio"
#define MyAppVersion "2.0.0"
#define MyAppPublisher "DataBase Studio"
#define MyAppURL "https://github.com/dbstudio"
#define MyAppExeName "DataBaseStudio.exe"
#define MyAppAssocName MyAppName + " File"
#define MyAppAssocExt ".db"
#define MyAppAssocKey StringChange(MyAppAssocName, " ", "") + MyAppAssocExt

[Setup]
; Identificador de aplicacion unico
AppId={{D37F89B2-9A44-4E5F-8E2B-9768A8D54249}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} v{#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
UninstallDisplayIcon={app}\{#MyAppExeName}
ChangesAssociations=yes
DisableProgramGroupPage=yes
; Icono del archivo instalador
SetupIconFile=..\src\main\resources\icon.ico
OutputDir=..\dist_installer
OutputBaseFilename=DataBaseStudio_Instalador_v2.0
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: checkedonce

[Files]
Source: "..\build\compose\binaries\main\app\DataBaseStudio\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\src\main\resources\icon.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\icon.ico"
Name: "{autoprograms}\Desinstalar {#MyAppName}"; Filename: "{uninstallexe}"; IconFilename: "{app}\icon.ico"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon; IconFilename: "{app}\icon.ico"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{app}"
