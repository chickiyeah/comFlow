import React, { useCallback, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, ActivityIndicator, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import Screen from '../components/Screen';
import Card from '../components/Card';
import Button from '../components/Button';
import { colors, spacing, radius } from '../theme/colors';
import { getMyProfile } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';

function Row({ icon, label, value }) {
  return (
    <View style={styles.row}>
      <View style={styles.rowIcon}>
        <Ionicons name={icon} size={18} color={colors.primary} />
      </View>
      <Text style={styles.rowLabel}>{label}</Text>
      <Text style={styles.rowValue}>{value ?? '—'}</Text>
    </View>
  );
}

export default function ProfileScreen() {
  const { signOut } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      const data = await getMyProfile();
      setProfile(data);
    } catch (e) {
      setProfile(null);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      let active = true;
      setLoading(true);
      load().finally(() => active && setLoading(false));
      return () => {
        active = false;
      };
    }, [load])
  );

  const confirmLogout = () => {
    Alert.alert('로그아웃', '정말 로그아웃 하시겠습니까?', [
      { text: '취소', style: 'cancel' },
      { text: '로그아웃', style: 'destructive', onPress: () => signOut() },
    ]);
  };

  const name = profile?.name || '학생';
  const initials = (name || 'CF').slice(0, 1);

  return (
    <Screen>
      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.heading}>프로필</Text>

        {loading ? (
          <View style={styles.center}>
            <ActivityIndicator color={colors.primary} size="large" />
          </View>
        ) : (
          <>
            <Card style={styles.profileCard}>
              <View style={styles.avatar}>
                <Text style={styles.avatarText}>{initials}</Text>
              </View>
              <Text style={styles.name}>{name}</Text>
              <Text style={styles.dept}>
                {profile?.department || '컴퓨터정보과'}
              </Text>
            </Card>

            <Card style={{ marginTop: spacing.md }}>
              <Row icon="card-outline" label="학번" value={profile?.studentId} />
              <Row icon="business-outline" label="학과" value={profile?.department} />
              <Row icon="layers-outline" label="학년" value={profile?.grade != null ? `${profile.grade}학년` : null} />
              <Row icon="calendar-outline" label="학기" value={profile?.semester != null ? `${profile.semester}학기` : null} last />
            </Card>

            <Button
              title="로그아웃"
              onPress={confirmLogout}
              variant="ghost"
              style={{ marginTop: spacing.lg }}
            />
          </>
        )}

        <Text style={styles.footer}>CampusFlow · campusflow.jvision.org</Text>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { padding: spacing.lg, paddingBottom: spacing.xl },
  heading: { fontSize: 26, fontWeight: '900', color: colors.text, marginBottom: spacing.lg },
  center: { alignItems: 'center', paddingVertical: spacing.xl * 2 },
  profileCard: { alignItems: 'center', paddingVertical: spacing.lg },
  avatar: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.sm,
  },
  avatarText: { color: colors.accent, fontSize: 30, fontWeight: '900' },
  name: { fontSize: 20, fontWeight: '900', color: colors.text },
  dept: { fontSize: 14, color: colors.textMuted, marginTop: 2 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: spacing.md,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.border,
  },
  rowIcon: {
    width: 34,
    height: 34,
    borderRadius: radius.sm,
    backgroundColor: colors.navyTint,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.md,
  },
  rowLabel: { fontSize: 14, color: colors.textMuted, fontWeight: '600' },
  rowValue: { marginLeft: 'auto', fontSize: 15, fontWeight: '700', color: colors.text },
  footer: { textAlign: 'center', color: colors.textMuted, marginTop: spacing.xl, fontSize: 12 },
});
