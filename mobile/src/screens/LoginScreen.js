import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Alert,
} from 'react-native';
import Screen from '../components/Screen';
import Button from '../components/Button';
import { colors, radius, spacing } from '../theme/colors';
import { useAuth } from '../context/AuthContext';

export default function LoginScreen() {
  const { signIn } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    if (!username.trim() || !password) {
      Alert.alert('알림', '학번과 비밀번호를 입력하세요.');
      return;
    }
    setLoading(true);
    try {
      await signIn(username.trim(), password);
      // 성공 시 AuthContext 의 token 변경 → RootNavigator 가 탭으로 전환
    } catch (e) {
      const msg =
        e?.response?.data?.message ||
        e?.message ||
        '로그인에 실패했습니다. 학번과 비밀번호를 확인하세요.';
      Alert.alert('로그인 실패', msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Screen style={styles.screen}>
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView
          contentContainerStyle={styles.scroll}
          keyboardShouldPersistTaps="handled"
        >
          <View style={styles.brand}>
            <View style={styles.logo}>
              <Text style={styles.logoText}>CF</Text>
            </View>
            <Text style={styles.title}>CampusFlow</Text>
            <Text style={styles.subtitle}>컴퓨터정보과 학과 통합 관리</Text>
          </View>

          <View style={styles.form}>
            <Text style={styles.label}>학번</Text>
            <TextInput
              style={styles.input}
              placeholder="학번을 입력하세요"
              placeholderTextColor={colors.textMuted}
              autoCapitalize="none"
              autoCorrect={false}
              value={username}
              onChangeText={setUsername}
            />

            <Text style={[styles.label, { marginTop: spacing.md }]}>비밀번호</Text>
            <TextInput
              style={styles.input}
              placeholder="비밀번호를 입력하세요"
              placeholderTextColor={colors.textMuted}
              secureTextEntry
              value={password}
              onChangeText={setPassword}
              onSubmitEditing={handleLogin}
            />

            <Button
              title="로그인"
              onPress={handleLogin}
              loading={loading}
              variant="accent"
              style={{ marginTop: spacing.lg }}
            />
          </View>

          <Text style={styles.footer}>campusflow.jvision.org</Text>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.primary },
  scroll: { flexGrow: 1, justifyContent: 'center', padding: spacing.lg },
  brand: { alignItems: 'center', marginBottom: spacing.xl },
  logo: {
    width: 72,
    height: 72,
    borderRadius: radius.lg,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.md,
  },
  logoText: { color: colors.primary, fontSize: 28, fontWeight: '900' },
  title: { color: colors.textInverse, fontSize: 30, fontWeight: '900' },
  subtitle: { color: '#c5d0ee', fontSize: 14, marginTop: 4 },
  form: {
    backgroundColor: colors.surface,
    borderRadius: radius.xl,
    padding: spacing.lg,
  },
  label: { color: colors.text, fontSize: 14, fontWeight: '700', marginBottom: 6 },
  input: {
    height: 52,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    fontSize: 16,
    color: colors.text,
    backgroundColor: colors.bg,
  },
  footer: { color: '#9fb0dc', textAlign: 'center', marginTop: spacing.xl, fontSize: 12 },
});
