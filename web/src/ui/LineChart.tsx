import { scaleLinear } from 'd3-scale'
import { area as d3area, curveMonotoneX, line as d3line } from 'd3-shape'

// d3-scale/d3-shape 로 좌표·path 만 계산하고 순수 SVG 로 렌더한다.
// 이 수학 레이어는 react-native-svg 에서도 동일하게 재사용 가능(통일 UI 목표).

export interface ChartSeries {
  values: number[]
  color: string
  label?: string
  fill?: boolean
}

interface LineChartProps {
  series: ChartSeries[]
  height?: number
  /** 기준선(예: 성과 비교의 100=손익분기). */
  baseline?: number
  /** y 축/마지막 값 포맷. */
  formatY?: (v: number) => string
}

const VIEW_W = 640
const PAD = { top: 14, right: 64, bottom: 22, left: 10 }

export function LineChart({ series, height = 220, baseline, formatY = (v) => v.toFixed(0) }: LineChartProps) {
  const usable = series.filter((s) => s.values.length > 0)
  if (!usable.length) return null

  const all = usable.flatMap((s) => s.values)
  if (baseline != null) all.push(baseline)
  let min = Math.min(...all)
  let max = Math.max(...all)
  if (min === max) {
    min -= 1
    max += 1
  }
  const margin = (max - min) * 0.08
  min -= margin
  max += margin

  const xMax = Math.max(...usable.map((s) => s.values.length - 1), 1)
  const x = scaleLinear([0, xMax], [PAD.left, VIEW_W - PAD.right])
  const y = scaleLinear([min, max], [height - PAD.bottom, PAD.top])

  const lineGen = d3line<number>()
    .x((_, i) => x(i))
    .y((d) => y(d))
    .curve(curveMonotoneX)
  const areaGen = d3area<number>()
    .x((_, i) => x(i))
    .y0(y(min))
    .y1((d) => y(d))
    .curve(curveMonotoneX)

  return (
    <svg viewBox={`0 0 ${VIEW_W} ${height}`} className="w-full" role="img" aria-label="라인 차트">
      {/* 기준선 */}
      {baseline != null && (
        <g>
          <line
            x1={PAD.left}
            x2={VIEW_W - PAD.right}
            y1={y(baseline)}
            y2={y(baseline)}
            stroke="#d1d5db"
            strokeWidth={1}
            strokeDasharray="4 4"
          />
          <text x={VIEW_W - PAD.right + 6} y={y(baseline) + 4} className="fill-gray-400 text-[11px]">
            {formatY(baseline)}
          </text>
        </g>
      )}

      {/* y 범위 라벨 */}
      <text x={VIEW_W - PAD.right + 6} y={y(max) + 4} className="fill-gray-300 text-[11px]">
        {formatY(max)}
      </text>
      <text x={VIEW_W - PAD.right + 6} y={y(min) + 4} className="fill-gray-300 text-[11px]">
        {formatY(min)}
      </text>

      {usable.map((s, si) => {
        const last = s.values[s.values.length - 1]
        return (
          <g key={si}>
            {s.fill && <path d={areaGen(s.values) ?? ''} fill={s.color} opacity={0.08} />}
            <path d={lineGen(s.values) ?? ''} fill="none" stroke={s.color} strokeWidth={2} />
            <circle cx={x(s.values.length - 1)} cy={y(last)} r={3} fill={s.color} />
            {s.label && (
              <text
                x={x(s.values.length - 1)}
                y={y(last) - 8}
                textAnchor="end"
                className="text-[11px] font-medium"
                fill={s.color}
              >
                {s.label}
              </text>
            )}
          </g>
        )
      })}
    </svg>
  )
}
