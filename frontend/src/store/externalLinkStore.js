import { create } from 'zustand'

// 외부 사이트 이동 확인 모달 상태. 어디서든 open(url)로 띄운다.
const useExternalLink = create((set) => ({
  url: null,
  open: (url) => set({ url }),
  close: () => set({ url: null }),
}))

export default useExternalLink
