import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import Card from './Card';
import { colors, spacing, radius } from '../theme/colors';

export default function StatCard({ icon, label, value, suffix, accent }) {
  return (
    <Card style={styles.card}>
      <View style={[styles.iconWrap, accent && { backgroundColor: colors.accent }]}>
        <Ionicons name={icon} size={20} color={accent ? colors.primary : colors.primary} />
      </View>
      <Text style={styles.label}>{label}</Text>
      <Text style={styles.value}>
        {value}
        {suffix ? <Text style={styles.suffix}>{suffix}</Text> : null}
      </Text>
    </Card>
  );
}

const styles = StyleSheet.create({
  card: { flex: 1, minHeight: 110 },
  iconWrap: {
    width: 38,
    height: 38,
    borderRadius: radius.sm,
    backgroundColor: colors.navyTint,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.sm,
  },
  label: { color: colors.textMuted, fontSize: 13, fontWeight: '600' },
  value: { color: colors.text, fontSize: 26, fontWeight: '800', marginTop: 2 },
  suffix: { fontSize: 14, fontWeight: '600', color: colors.textMuted },
});
