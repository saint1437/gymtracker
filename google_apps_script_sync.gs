/**
 * Необязательный fallback для импорта данных в Google Sheets через Web App.
 * Основной режим GymTracker — прямой Google Drive API.
 *
 * 1. Откройте Google Sheet «Дневник_тренировок_Фулбоди».
 * 2. Расширения -> Apps Script, вставьте этот файл и сохраните.
 * 3. Deploy -> New deployment -> Web app -> Execute as Me -> Anyone with link.
 * 4. POST JSON из клиента на URL Web App.
 */

const SHEET_NAME = 'Тренировки';
const DRIVE_FOLDER_NAME = 'Тренировки';
const HISTORY_FILE_NAME = 'workout_history.json';
const HEADERS = [
  'Дата', 'День цикла', 'Упражнение', 'Группа мышц', 'Целевые подходы',
  'Целевые повторения', 'RIR (Запас)', 'Подход 1 (Вес x Повт)',
  'Подход 2 (Вес x Повт)', 'Подход 3 (Вес x Повт)',
  'Заметки по технике и самочувствию'
];

function doPost(e) {
  try {
    const payload = JSON.parse(e.postData.contents);
    const spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
    const sheet = spreadsheet.getSheetByName(SHEET_NAME) || spreadsheet.insertSheet(SHEET_NAME);
    ensureHeaders_(sheet);
    const sessions = payload.sessions || (payload.session ? [payload.session] : []);
    sessions.forEach(session => appendSession_(sheet, session));
    saveHistory_(payload);
    return jsonResponse_({ ok: true, sessions: sessions.length });
  } catch (error) {
    return jsonResponse_({ ok: false, error: String(error) });
  }
}

function ensureHeaders_(sheet) {
  if (sheet.getLastRow() === 0) sheet.getRange(1, 1, 1, HEADERS.length).setValues([HEADERS]);
}

function appendSession_(sheet, session) {
  const sessionId = session.id || `${session.startedAt || Date.now()}-${session.title || 'workout'}`;
  const properties = PropertiesService.getScriptProperties();
  const processedKey = `processed_${sessionId}`;
  if (properties.getProperty(processedKey)) return;
  const date = session.startedAt ? new Date(Number(session.startedAt)) : new Date();
  (session.exercises || []).forEach(exercise => {
    const sets = (exercise.sets || []).slice(0, 3).map(set => `${set.weightKg || 0} кг x ${set.reps || 0}`);
    sheet.appendRow([
      date, session.cycleType || session.title || 'Ручная', exercise.name || '', exercise.group || '',
      exercise.targetSets || '', `${exercise.repMin || ''}-${exercise.repMax || ''}`, exercise.targetRir || '',
      sets[0] || '', sets[1] || '', sets[2] || '', exercise.note || ''
    ]);
  });
  properties.setProperty(processedKey, new Date().toISOString());
}

function saveHistory_(payload) {
  const folder = getOrCreateFolder_(DRIVE_FOLDER_NAME);
  const content = JSON.stringify(payload, null, 2);
  const files = folder.getFilesByName(HISTORY_FILE_NAME);
  if (files.hasNext()) files.next().setContent(content);
  else folder.createFile(HISTORY_FILE_NAME, content, MimeType.JSON);
}

function getOrCreateFolder_(name) {
  const folders = DriveApp.getFoldersByName(name);
  return folders.hasNext() ? folders.next() : DriveApp.createFolder(name);
}

function jsonResponse_(object) {
  return ContentService.createTextOutput(JSON.stringify(object)).setMimeType(ContentService.MimeType.JSON);
}