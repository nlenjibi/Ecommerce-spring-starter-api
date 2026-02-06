import React from 'react';

type Step = { key: string; label: string; timestamp?: string };

export default function OrderStatusTracker({ steps, currentKey }: { steps: Step[]; currentKey: string }) {
  const currentIndex = steps.findIndex((s) => s.key === currentKey);
  return (
    <div className="flex items-center gap-3 overflow-auto">
      {steps.map((s, i) => {
        const done = i <= currentIndex;
        return (
          <div key={s.key} className="flex items-center gap-3">
            <div className={`w-8 h-8 rounded-full grid place-items-center ${done ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-500'}`}>{i + 1}</div>
            <div className="min-w-[80px]">
              <div className={`text-sm ${done ? 'text-gray-900' : 'text-gray-500'}`}>{s.label}</div>
              {s.timestamp && <div className="text-xs text-gray-400">{new Date(s.timestamp).toLocaleString()}</div>}
            </div>
            {i < steps.length - 1 && <div className={`h-1 flex-1 ${i < currentIndex ? 'bg-blue-400' : 'bg-gray-200'}`} />}
          </div>
        );
      })}
    </div>
  );
}
