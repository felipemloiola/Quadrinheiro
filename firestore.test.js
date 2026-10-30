const { test, before, after, beforeEach } = require('node:test');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');
const { readFileSync } = require('node:fs');

let testEnv;

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8085').split(':');
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-no-project',
    firestore: {
      rules: readFileSync('firestore.rules', 'utf8'),
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

const validPreferences = {
  themeId: 'SYSTEM_MONO',
  colorScheme: 'system',
  glassLevel: 26,
  glassVariant: 'regular',
  edgeRefraction: true,
  glassIntensity: 1.0,
  glassBlurIntensity: 1.0,
  enableLensRefraction: true,
  enableChromaticAberration: true,
  glassOpacity: 1.0,
  backgroundExtension: true,
  showBottomAccessory: true,
  showFloatingMenu: true,
  minimizeTabBarOnScroll: true,
  reduceTransparency: false,
  increaseContrast: false,
  boldText: false,
  dynamicTypeScale: 100,
  reduceMotion: false,
  onOffLabels: false,
  shelfColumns: 3,
  showProgressOnCovers: true,
  showRatingsOnGrid: true,
  spine3dEffect: true,
  defaultShelfSort: 'RECENT',
  shelfLayout: 'grid',
  defaultFormat: 'Encadernado (TPB)',
  searchLanguage: 'ALL',
  searchProvider: 'MULTI',
  hdCovers: true,
  spoilerProtection: false,
  halfStarRatings: true,
  allowRereads: true,
  autoFillReadDate: true,
  showPagesInDiary: true,
  quickReviewTags: ['Roteiro impecável', 'Arte incrível'],
};

test('User profile with avatarUrl, goals, and preferences succeeds for owner and fails for other users', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const bobDb = testEnv.authenticatedContext('bob').firestore();

  const aliceProfile = {
    ownerId: 'alice',
    name: 'Alice',
    handle: '@alice_hq',
    normalizedHandle: 'alice_hq',
    bio: 'Fã de quadrinhos',
    favoriteUniverse: 'DC / Vertigo',
    avatarUrl: 'data:image/jpeg;base64,abc123',
    avatarPreset: 'indigo',
    yearlyGoal: 24,
    dailyPagesGoal: 30,
    showGoalOnHome: true,
    activeReadingComicId: 'init-batman-fraction-2',
    preferences: validPreferences,
    updatedAtMillis: 1700000000000,
    updatedAt: new Date(),
  };

  await assertSucceeds(aliceDb.doc('users/alice').set(aliceProfile));
  await assertSucceeds(aliceDb.doc('users/alice').get());
  await assertFails(bobDb.doc('users/alice').get());
  await assertFails(bobDb.doc('users/alice').set(aliceProfile));
});

test('Unique username reservation prevents two users from claiming the same handleKey', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const bobDb = testEnv.authenticatedContext('bob').firestore();

  await assertSucceeds(
    aliceDb.doc('usernames/quadrinheiro_top').set({
      ownerId: 'alice',
      handle: '@quadrinheiro_top',
      normalizedHandle: 'quadrinheiro_top',
      updatedAtMillis: 1700000000000,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(bobDb.doc('usernames/quadrinheiro_top').get());

  await assertFails(
    bobDb.doc('usernames/quadrinheiro_top').set({
      ownerId: 'bob',
      handle: '@quadrinheiro_top',
      normalizedHandle: 'quadrinheiro_top',
      updatedAtMillis: 1700000000001,
      updatedAt: new Date(),
    })
  );
  await assertFails(bobDb.doc('usernames/quadrinheiro_top').delete());
  await assertSucceeds(aliceDb.doc('usernames/quadrinheiro_top').delete());
});
