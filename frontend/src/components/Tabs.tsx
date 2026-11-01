/** iOS-style segmented control. */
export function Tabs<T extends string>({ tabs, value, onChange }: {
  tabs: { value: T; label: string; count?: number }[]
  value: T
  onChange: (v: T) => void
}) {
  return (
    <div className="mb-6 inline-flex max-w-full gap-0.5 overflow-x-auto rounded-xl bg-canvas-200 p-0.5" role="tablist">
      {tabs.map((t) => (
        <button
          key={t.value}
          role="tab"
          aria-selected={value === t.value}
          onClick={() => onChange(t.value)}
          className={`flex shrink-0 items-center gap-1.5 rounded-[10px] px-4 py-1.5 text-[13px] font-semibold transition-all duration-150 ${
            value === t.value
              ? 'bg-canvas-50 text-ink-900 shadow-[0_1px_3px_rgba(0,0,0,0.12)]'
              : 'text-ink-600 hover:text-ink-900'
          }`}
        >
          {t.label}
          {t.count !== undefined && t.count > 0 && (
            <span className="rounded-full bg-danger-500 px-1.5 text-[11px] leading-[18px] text-white tabular-nums">{t.count}</span>
          )}
        </button>
      ))}
    </div>
  )
}
