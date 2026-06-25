// web/src/ui/LineChart.tsx 의 RN 포트. d3-scale/d3-shape 수학은 동일하게 재사용하고
// 렌더만 react-native-svg 로 한다(통일 UI: 같은 좌표·path 로직, 다른 렌더 타깃).

import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import { scaleLinear } from 'd3-scale';
import { area as d3area, curveMonotoneX, line as d3line } from 'd3-shape';
import { Circle, Line, Path, Rect, Svg, Text as SvgText } from 'react-native-svg';

export interface ChartSeries {
  values: number[];
  color: string;
  label?: string;
  fill?: boolean;
}

interface LineChartProps {
  series: ChartSeries[];
  /** RN 은 명시 너비 필요(보통 컨테이너 onLayout 값). */
  width: number;
  height?: number;
  baseline?: number;
  formatY?: (v: number) => string;
  /** 각 데이터 포인트의 x 라벨(날짜 'YYYY-MM-DD'). x축 틱·터치 툴팁에 사용. */
  xLabels?: string[];
  formatX?: (label: string) => string;
}

const PAD = { top: 14, right: 64, bottom: 30, left: 10 };
const X_TICKS = 5;

export function LineChart({
  series,
  width,
  height = 220,
  baseline,
  formatY = v => v.toFixed(0),
  xLabels,
  formatX,
}: LineChartProps) {
  const [hover, setHover] = useState<number | null>(null);

  const usable = series.filter(s => s.values.length > 0);
  if (!usable.length || width <= 0) {
    return null;
  }

  const all = usable.flatMap(s => s.values);
  if (baseline != null) {
    all.push(baseline);
  }
  let min = Math.min(...all);
  let max = Math.max(...all);
  if (min === max) {
    min -= 1;
    max += 1;
  }
  const margin = (max - min) * 0.08;
  min -= margin;
  max += margin;

  const xMax = Math.max(...usable.map(s => s.values.length - 1), 1);
  const x = scaleLinear([0, xMax], [PAD.left, width - PAD.right]);
  const y = scaleLinear([min, max], [height - PAD.bottom, PAD.top]);

  const lineGen = d3line<number>()
    .x((_, i) => x(i))
    .y(d => y(d))
    .curve(curveMonotoneX);
  const areaGen = d3area<number>()
    .x((_, i) => x(i))
    .y0(y(min))
    .y1(d => y(d))
    .curve(curveMonotoneX);

  const labels = xLabels ?? [];
  const multiYear = labels.length > 1 && labels[0].slice(0, 4) !== labels[labels.length - 1].slice(0, 4);
  const fmtX = formatX ?? ((s: string) => (multiYear ? s.slice(0, 4) : s.slice(0, 7)));
  const ticks =
    labels.length > 1
      ? Array.from({ length: X_TICKS }, (_, i) => {
          const idx = Math.round((i / (X_TICKS - 1)) * (labels.length - 1));
          return { idx, label: fmtX(labels[idx]) };
        })
      : [];
  const axisY = height - PAD.bottom;

  // RNGH Pan: activeOffsetX 로 가로 드래그만 활성 → 세로 스크롤은 부모 ScrollView 로 통과(iOS·Android 모두 동작).
  const setHoverAt = (lx: number) =>
    setHover(Math.max(0, Math.min(xMax, Math.round(x.invert(lx)))));
  const panGesture = Gesture.Pan()
    .activeOffsetX([-10, 10])
    .runOnJS(true)
    .onBegin(e => setHoverAt(e.x))
    .onUpdate(e => setHoverAt(e.x))
    .onFinalize(() => setHover(null));

  const hoverItems =
    hover != null
      ? usable
          .filter(s => hover < s.values.length)
          .map(s => ({ color: s.color, label: s.label, value: formatY(s.values[hover]) }))
      : [];
  const boxW = Math.min(140, width - PAD.left - 8);
  const boxH = 10 + 14 + hoverItems.length * 15;
  let tx = hover != null ? x(hover) + 10 : 0;
  if (tx + boxW > width) {
    tx = (hover != null ? x(hover) : 0) - boxW - 10;
  }
  if (tx < 0) {
    tx = 4;
  }

  return (
    // 터치는 Svg 위에 덮은 투명 오버레이 View 가 받는다 — iOS 에서 Svg 네이티브 뷰가 터치를
    // 가로채는 react-native-svg 이슈 회피(Android·iOS 모두 동작).
    <View style={{ width, height }}>
      <Svg width={width} height={height}>
        <Line x1={PAD.left} x2={width - PAD.right} y1={axisY} y2={axisY} stroke="#eae4da" strokeWidth={1} />

      {baseline != null && (
        <>
          <Line
            x1={PAD.left}
            x2={width - PAD.right}
            y1={y(baseline)}
            y2={y(baseline)}
            stroke="#d1d5db"
            strokeWidth={1}
            strokeDasharray="4,4"
          />
          <SvgText x={width - PAD.right + 6} y={y(baseline) + 4} fill="#9ca3af" fontSize={11}>
            {formatY(baseline)}
          </SvgText>
        </>
      )}

      <SvgText x={width - PAD.right + 6} y={y(max) + 4} fill="#d1d5db" fontSize={11}>
        {formatY(max)}
      </SvgText>
      <SvgText x={width - PAD.right + 6} y={y(min) + 4} fill="#d1d5db" fontSize={11}>
        {formatY(min)}
      </SvgText>

      {ticks.map((t, i) => (
        <Svg key={`t${i}`}>
          <Line x1={x(t.idx)} x2={x(t.idx)} y1={axisY} y2={axisY + 4} stroke="#d6cfc2" strokeWidth={1} />
          <SvgText
            x={x(t.idx)}
            y={axisY + 16}
            fill="#9ca3af"
            fontSize={11}
            textAnchor={i === 0 ? 'start' : i === ticks.length - 1 ? 'end' : 'middle'}>
            {t.label}
          </SvgText>
        </Svg>
      ))}

      {usable.map((s, si) => {
        const last = s.values[s.values.length - 1];
        return (
          <Svg key={si}>
            {s.fill && <Path d={areaGen(s.values) ?? ''} fill={s.color} fillOpacity={0.08} />}
            <Path d={lineGen(s.values) ?? ''} fill="none" stroke={s.color} strokeWidth={2} />
            <Circle cx={x(s.values.length - 1)} cy={y(last)} r={3} fill={s.color} />
            {s.label && (
              <SvgText
                x={x(s.values.length - 1)}
                y={y(last) - 8}
                fill={s.color}
                fontSize={11}
                fontWeight="500"
                textAnchor="end">
                {s.label}
              </SvgText>
            )}
          </Svg>
        );
      })}

      {/* 터치 크로스헤어 + 툴팁 */}
      {hover != null && (
        <Svg>
          <Line
            x1={x(hover)}
            x2={x(hover)}
            y1={PAD.top}
            y2={axisY}
            stroke="#b5703c"
            strokeWidth={1}
            strokeDasharray="3,3"
            opacity={0.5}
          />
          {usable.map((s, si) =>
            hover < s.values.length ? (
              <Circle key={`h${si}`} cx={x(hover)} cy={y(s.values[hover])} r={3.5} fill={s.color} stroke="#fff" strokeWidth={1} />
            ) : null,
          )}
          <Rect x={tx} y={PAD.top} width={boxW} height={boxH} rx={6} fill="#fff" stroke="#eae4da" />
          <SvgText x={tx + 8} y={PAD.top + 15} fill="#6b7280" fontSize={10}>
            {labels[hover] ?? `#${hover}`}
          </SvgText>
          {hoverItems.map((it, i) => (
            <SvgText key={`hi${i}`} x={tx + 8} y={PAD.top + 15 + (i + 1) * 15} fill={it.color} fontSize={11} fontWeight="600">
              {(it.label ? it.label + ' ' : '') + it.value}
            </SvgText>
          ))}
        </Svg>
      )}
      </Svg>
      <GestureDetector gesture={panGesture}>
        <View style={StyleSheet.absoluteFill} />
      </GestureDetector>
    </View>
  );
}
