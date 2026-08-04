import React, { useCallback, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import Screen from '../components/Screen';
import Card from '../components/Card';
import StatCard from '../components/StatCard';
import { colors, spacing, radius } from '../theme/colors';
import {
  getMyGrades,
  getMyAttendance,
  getTodaySchedule,
} from '../api/endpoints';

export default function DashboardScreen() {
  const [grades, setGrades] = useState(null);
  const [attendance, setAttendance] = useState(null);
  const [today, setToday] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    // 각 호출을 독립적으로 처리 — 하나 실패해도 나머지는 표시
    const results = await Promise.allSettled([
      getMyGrades(),
      getMyAttendance(),
      getTodaySchedule(),
    ]);
    if (results[0].status === 'fulfilled') setGrades(results[0].value);
    if (results[1].status === 'fulfilled') setAttendance(results[1].value);
    if (results[2].status === 'fulfilled') {
      const v = results[2].value;
      setToday(Array.isArray(v) ? v : v?.classes || v?.items || []);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      let active = true;
      setLoading(true);
      load().finally(() => {
        if (active) setLoading(false);
      });
      return () => {
        active = false;
      };
    }, [load])
  );

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  }, [load]);

  const fmt = (v, fallback = '—') =>
    v === null || v === undefined || v === '' ? fallback : v;

  const attRate =
    attendance?.attendanceRate ??
    attendance?.rate ??
    attendance?.attendance ??
    null;

  return (
    <Screen>
      <ScrollView
        contentContainerStyle={styles.scroll}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}
      >
        <Text style={styles.heading}>대시보드</Text>
        <Text style={styles.sub}>오늘의 학업 현황을 한눈에 확인하세요.</Text>

        {loading ? (
          <View style={styles.loadingBox}>
            <ActivityIndicator color={colors.primary} size="large" />
          </View>
        ) : (
          <>
            <View style={styles.row}>
              <StatCard
                icon="school-outline"
                label="평점 (GPA)"
                value={fmt(grades?.gpa)}
                accent
              />
              <View style={{ width: spacing.md }} />
              <StatCard
                icon="ribbon-outline"
                label="이수 학점"
                value={fmt(grades?.totalCredits)}
                suffix=" 학점"
              />
            </View>

            <View style={[styles.row, { marginTop: spacing.md }]}>
              <StatCard
                icon="checkmark-circle-outline"
                label="출석률"
                value={attRate === null ? '—' : `${attRate}`}
                suffix={attRate === null ? '' : ' %'}
              />
              <View style={{ width: spacing.md }} />
              <StatCard
                icon="time-outline"
                label="오늘 강의"
                value={today.length}
                suffix=" 개"
              />
            </View>

            <Card style={{ marginTop: spacing.lg }}>
              <View style={styles.cardHead}>
                <Ionicons name="calendar-outline" size={18} color={colors.primary} />
                <Text style={styles.cardTitle}>오늘의 강의</Text>
              </View>
              {today.length === 0 ? (
                <Text style={styles.empty}>오늘 예정된 강의가 없습니다.</Text>
              ) : (
                today.map((c, i) => (
                  <View key={i} style={styles.lecture}>
                    <View style={styles.dot} />
                    <View style={{ flex: 1 }}>
                      <Text style={styles.lectureName}>
                        {c.subject || c.name || c.courseName || c.title || '강의'}
                      </Text>
                      <Text style={styles.lectureMeta}>
                        {[c.time || c.period, c.room || c.location, c.professor]
                          .filter(Boolean)
                          .join(' · ') || '시간 정보 없음'}
                      </Text>
                    </View>
                  </View>
                ))
              )}
            </Card>
          </>
        )}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { padding: spacing.lg, paddingBottom: spacing.xl },
  heading: { fontSize: 26, fontWeight: '900', color: colors.text },
  sub: { color: colors.textMuted, marginTop: 4, marginBottom: spacing.lg },
  row: { flexDirection: 'row' },
  loadingBox: { paddingVertical: spacing.xl * 2, alignItems: 'center' },
  cardHead: { flexDirection: 'row', alignItems: 'center', marginBottom: spacing.sm },
  cardTitle: { fontSize: 16, fontWeight: '800', color: colors.text, marginLeft: 6 },
  empty: { color: colors.textMuted, paddingVertical: spacing.md },
  lecture: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  dot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: colors.accent,
    marginRight: spacing.md,
  },
  lectureName: { fontSize: 15, fontWeight: '700', color: colors.text },
  lectureMeta: { fontSize: 13, color: colors.textMuted, marginTop: 2 },
});
