// web/src/ui/LineChart.tsx 의 RN 포트. d3-scale/d3-shape 수학은 동일하게 재사용하고
// 렌더만 react-native-svg 로 한다(통일 UI: 같은 좌표·path 로직, 다른 렌더 타깃).

import { scaleLinear } from 'd3-scale';
import { area as d3area, curveMonotoneX, line as d3line } from 'd3-shape';
import { Circle, Line, Path, Svg, Text as SvgText } from 'react-native-svg';

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
  /** 각 데이터 포인트의 x 라벨(날짜 'YYYY-MM-DD'). 일부만 x축 틱으로. */
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

  return (
    <Svg width={width} height={height}>
      {/* x축 선 */}
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
    </Svg>
  );
}
