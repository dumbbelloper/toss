// 곳간 로고 마크 (react-native-svg). web/src/ui/Logo.tsx 와 동일 path 공유.
import { Path, Rect, Svg } from 'react-native-svg';

export function LogoMark({ size = 24, color = '#3182f6' }: { size?: number; color?: string }) {
  return (
    <Svg width={size} height={size} viewBox="0 0 32 32">
      {/* 창고 지붕 */}
      <Path d="M16 3 L29 12 L3 12 Z" fill={color} />
      {/* 갈무리되는 자산(상승 막대) */}
      <Rect x={8} y={18} width={4} height={8} rx={1} fill={color} />
      <Rect x={14} y={16} width={4} height={10} rx={1} fill={color} />
      <Rect x={20} y={14} width={4} height={12} rx={1} fill={color} />
    </Svg>
  );
}
