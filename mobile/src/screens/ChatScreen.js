import React, { useRef, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  TouchableOpacity,
  ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import Screen from '../components/Screen';
import { colors, radius, spacing } from '../theme/colors';
import { komjeongChat } from '../api/endpoints';

let idSeq = 0;
const nextId = () => `m${idSeq++}`;

export default function ChatScreen() {
  const [messages, setMessages] = useState([
    {
      id: nextId(),
      role: 'bot',
      text: '안녕하세요! 컴퓨터정보과 AI 도우미입니다. 자격증·취업·진로 등 학과 관련 무엇이든 물어보세요.',
    },
  ]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const listRef = useRef(null);

  const scrollToEnd = () => {
    setTimeout(() => listRef.current?.scrollToEnd({ animated: true }), 50);
  };

  const send = async () => {
    const text = input.trim();
    if (!text || sending) return;
    setInput('');
    const userMsg = { id: nextId(), role: 'user', text };
    setMessages((prev) => [...prev, userMsg]);
    setSending(true);
    scrollToEnd();
    try {
      const data = await komjeongChat(text);
      const answer = data?.answer || data?.response || '응답을 받지 못했습니다.';
      setMessages((prev) => [...prev, { id: nextId(), role: 'bot', text: answer }]);
    } catch (e) {
      setMessages((prev) => [
        ...prev,
        {
          id: nextId(),
          role: 'bot',
          text: '죄송합니다. 답변 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
          error: true,
        },
      ]);
    } finally {
      setSending(false);
      scrollToEnd();
    }
  };

  const renderItem = ({ item }) => {
    const isUser = item.role === 'user';
    return (
      <View style={[styles.bubbleRow, isUser ? styles.rowRight : styles.rowLeft]}>
        {!isUser && (
          <View style={styles.avatar}>
            <Ionicons name="sparkles" size={14} color={colors.primary} />
          </View>
        )}
        <View
          style={[
            styles.bubble,
            isUser ? styles.userBubble : styles.botBubble,
            item.error && styles.errorBubble,
          ]}
        >
          <Text style={[styles.bubbleText, isUser && { color: colors.textInverse }]}>
            {item.text}
          </Text>
        </View>
      </View>
    );
  };

  return (
    <Screen>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>AI 챗</Text>
        <Text style={styles.headerSub}>학과 자료 기반 도우미</Text>
      </View>
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}
      >
        <FlatList
          ref={listRef}
          data={messages}
          keyExtractor={(m) => m.id}
          renderItem={renderItem}
          contentContainerStyle={styles.list}
          onContentSizeChange={scrollToEnd}
        />
        {sending && (
          <View style={styles.typing}>
            <ActivityIndicator size="small" color={colors.primary} />
            <Text style={styles.typingText}>답변 생성 중…</Text>
          </View>
        )}
        <View style={styles.inputBar}>
          <TextInput
            style={styles.input}
            placeholder="메시지를 입력하세요"
            placeholderTextColor={colors.textMuted}
            value={input}
            onChangeText={setInput}
            multiline
            onSubmitEditing={send}
          />
          <TouchableOpacity
            style={[styles.sendBtn, (!input.trim() || sending) && styles.sendBtnDisabled]}
            onPress={send}
            disabled={!input.trim() || sending}
            activeOpacity={0.85}
          >
            <Ionicons name="arrow-up" size={22} color={colors.primary} />
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: {
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.sm,
    paddingBottom: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
    backgroundColor: colors.surface,
  },
  headerTitle: { fontSize: 22, fontWeight: '900', color: colors.text },
  headerSub: { fontSize: 13, color: colors.textMuted, marginTop: 2 },
  list: { padding: spacing.md, paddingBottom: spacing.md },
  bubbleRow: { flexDirection: 'row', marginBottom: spacing.md, alignItems: 'flex-end' },
  rowLeft: { justifyContent: 'flex-start' },
  rowRight: { justifyContent: 'flex-end' },
  avatar: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 6,
  },
  bubble: { maxWidth: '78%', borderRadius: radius.lg, padding: spacing.md },
  userBubble: { backgroundColor: colors.primary, borderBottomRightRadius: 4 },
  botBubble: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderBottomLeftRadius: 4,
  },
  errorBubble: { borderColor: colors.danger },
  bubbleText: { fontSize: 15, lineHeight: 21, color: colors.text },
  typing: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: spacing.lg, paddingBottom: spacing.sm },
  typingText: { color: colors.textMuted, marginLeft: 8, fontSize: 13 },
  inputBar: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    padding: spacing.sm,
    paddingHorizontal: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.surface,
  },
  input: {
    flex: 1,
    maxHeight: 120,
    minHeight: 44,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.lg,
    paddingHorizontal: spacing.md,
    paddingTop: 12,
    paddingBottom: 12,
    fontSize: 15,
    color: colors.text,
    backgroundColor: colors.bg,
  },
  sendBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: spacing.sm,
  },
  sendBtnDisabled: { opacity: 0.4 },
});
