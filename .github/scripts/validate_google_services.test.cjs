const { test } = require('node:test');
const assert = require('node:assert/strict');
const { validateGoogleServices } = require('./validate_google_services.cjs');

// Synthetic schema fixtures, never used as application configuration.
function fixture() {
  return {
    project_info: { project_id: 'gen-lang-client-0375936605', project_number: '123456' },
    client: [{
      client_info: { mobilesdk_app_id: '1:123456:android:abc', android_client_info: { package_name: 'com.aistudio.quadrinheiro.lqglas' } },
      api_key: [{ current_key: 'AIza' + 'a'.repeat(35) }],
      oauth_client: [
        { client_type: 3, client_id: '123456-fixture.apps.googleusercontent.com' },
        { client_type: 1, android_info: { package_name: 'com.aistudio.quadrinheiro.lqglas', certificate_hash: 'AB'.repeat(20) } }
      ]
    }]
  };
}
test('accepts matching package, Web client and release certificate', () => {
  assert.equal(validateGoogleServices(fixture(), 'AB:'.repeat(19) + 'AB').projectId, 'gen-lang-client-0375936605');
});
test('rejects missing Web OAuth client even when an Android client exists', () => {
  const config = fixture(); config.client[0].oauth_client.shift();
  assert.throws(() => validateGoogleServices(config), /OAuth Web/);
});
test('rejects another application package', () => {
  const config = fixture(); config.client[0].client_info.android_client_info.package_name = 'wrong.package';
  assert.throws(() => validateGoogleServices(config), /pacote/);
});
test('rejects another Firebase project even when its package matches', () => {
  const config = fixture(); config.project_info.project_id = 'another-project';
  assert.throws(() => validateGoogleServices(config), /gen-lang-client/);
});
test('rejects debug-only OAuth configuration for a release signature', () => {
  assert.throws(() => validateGoogleServices(fixture(), 'CD'.repeat(20)), /SHA-1/);
});
test('rejects the Firestore database name used as a project ID', () => {
  const config = fixture(); config.project_info.project_id = 'ai-studio-android-quadrinh-d0141987-7d2c-491d-b885-fc910c102b71';
  assert.throws(() => validateGoogleServices(config), /ID do banco/);
});
