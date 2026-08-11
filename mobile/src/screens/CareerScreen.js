import React, { useCallback, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import Screen from '../components/Screen';
import Card from '../components/Card';
import { colors, spacing, radius } from '../theme/colors';
import { getCareerActivities } from '../api/endpoints';

const TYPE_LABEL = {
  CERTIFICATE: '자격증',
  CERT: '자격증',
  LANGUAGE: '어학',
  INTERN: '인턴',
  INTERNSHIP: '인턴',
  PROJECT: '프로젝트',
  ACTIVITY: '활동',
};

const typeIcon = (t) => {
  const k = (t || '').toUpperCase();
  if (k.includes('CERT')) return 'ribbon-outline';
  if (k.includes('LANG')) return 'language-outline';
  if (k.includes('INTERN')) return 'briefcase-outline';
  if (k.includes('PROJECT')) return 'construct-outline';
  return 'flag-outline';
};

export default function CareerScreen() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(false);

  const load = useCallback(async () => {
    setError(false);
    try {
      const data = await getCareerActivities();
      setItems(Array.isArray(data) ? data : data?.activities || data?.items || []);
    } catch (e) {
      setError(true);
      setItems([]);
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

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  }, [load]);

  const renderItem = ({ item }) => {
    const type = item.type || item.category;
    return (
      <Card style={styles.item}>
        <View style={styles.iconWrap}>
          <Ionicons name={typeIcon(type)} size={20} color={colors.primary} />
        </View>
        <View style={{ flex: 1 }}>
          <Text style={styles.itemTitle}>
            {item.title || item.name || '활동'}
          </Text>
          {(item.organization || item.issuer) && (
            <Text style={styles.itemMeta}>{item.organization || item.issuer}</Text>
          )}
          <View style={styles.tags}>
            {type ? (
              <View style={styles.tag}>
                <Text style={styles.tagText}>{TYPE_LABEL[(type || '').toUpperCase()] || type}</Text>
              </View>
            ) : null}
            {(item.date || item.acquiredDate || item.endDate) && (
              <Text style={styles.date}>{item.date || item.acquiredDate || item.endDate}</Text>
            )}
          </View>
        </View>
      </Card>
    );
  };

  return (
    <Screen>
      <View style={styles.header}>
        <Text style={styles.heading}>진로</Text>
        <Text style={styles.sub}>나의 취업 준비 활동</Text>
      </View>
      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator color={colors.primary} size="large" />
        </View>
      ) : (
        <FlatList
          data={items}
          keyExtractor={(it, i) => String(it.id ?? i)}
          renderItem={renderItem}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}
          ListEmptyComponent={
            <View style={styles.center}>
              <Ionicons name="briefcase-outline" size={40} color={colors.textMuted} />
              <Text style={styles.empty}>
                {error ? '활동을 불러오지 못했습니다.' : '등록된 취업 준비 활동이 없습니다.'}
              </Text>
            </View>
          }
        />
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { paddingHorizontal: spacing.lg, paddingTop: spacing.sm, paddingBottom: spacing.sm },
  heading: { fontSize: 26, fontWeight: '900', color: colors.text },
  sub: { color: colors.textMuted, marginTop: 4 },
  list: { padding: spacing.lg, paddingTop: spacing.sm },
  center: { alignItems: 'center', justifyContent: 'center', paddingVertical: spacing.xl * 2 },
  empty: { color: colors.textMuted, marginTop: spacing.sm },
  item: { flexDirection: 'row', alignItems: 'center', marginBottom: spacing.md },
  iconWrap: {
    width: 44,
    height: 44,
    borderRadius: radius.md,
    backgroundColor: colors.navyTint,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.md,
  },
  itemTitle: { fontSize: 16, fontWeight: '800', color: colors.text },
  itemMeta: { fontSize: 13, color: colors.textMuted, marginTop: 2 },
  tags: { flexDirection: 'row', alignItems: 'center', marginTop: 6, gap: 8 },
  tag: {
    backgroundColor: colors.accent,
    borderRadius: radius.pill,
    paddingHorizontal: 10,
    paddingVertical: 3,
  },
  tagText: { color: colors.primary, fontSize: 12, fontWeight: '700' },
  date: { fontSize: 12, color: colors.textMuted },
});
