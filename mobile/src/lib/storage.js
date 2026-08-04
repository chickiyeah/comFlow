// 토큰 저장소 추상화.
// expo-secure-store 가 있으면 사용하고, 없으면 AsyncStorage 로 폴백한다.
import { TOKEN_KEY } from '../config/env';

let secureStore = null;
try {
  // eslint-disable-next-line global-require
  secureStore = require('expo-secure-store');
} catch (e) {
  secureStore = null;
}

let asyncStorage = null;
if (!secureStore) {
  try {
    // eslint-disable-next-line global-require
    asyncStorage = require('@react-native-async-storage/async-storage').default;
  } catch (e) {
    asyncStorage = null;
  }
}

// SecureStore 가 웹/일부 환경에서 미지원일 수 있어 isAvailable 체크
async function secureAvailable() {
  if (!secureStore) return false;
  try {
    if (typeof secureStore.isAvailableAsync === 'function') {
      return await secureStore.isAvailableAsync();
    }
    return true;
  } catch (e) {
    return false;
  }
}

export async function saveToken(token) {
  if (await secureAvailable()) {
    return secureStore.setItemAsync(TOKEN_KEY, token);
  }
  if (asyncStorage) return asyncStorage.setItem(TOKEN_KEY, token);
  return null;
}

export async function getToken() {
  if (await secureAvailable()) {
    return secureStore.getItemAsync(TOKEN_KEY);
  }
  if (asyncStorage) return asyncStorage.getItem(TOKEN_KEY);
  return null;
}

export async function deleteToken() {
  if (await secureAvailable()) {
    return secureStore.deleteItemAsync(TOKEN_KEY);
  }
  if (asyncStorage) return asyncStorage.removeItem(TOKEN_KEY);
  return null;
}
