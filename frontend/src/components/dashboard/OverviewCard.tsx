import React from 'react';

export type OverviewCardProps = {
  title: string;
  value: string | number;
  delta?: string;
  icon?: React.ReactNode;
  onClick?: () => void;
};

export default function OverviewCard({ title, value, delta, icon, onClick }: OverviewCardProps) {
  return (
    <div className="bg-white rounded-2xl shadow-sm p-4 flex items-center justify-between"> 
      <div className="flex items-center gap-4">
        <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 grid place-items-center">{icon}</div>
        <div>
          <div className="text-sm text-gray-500">{title}</div>
          <div className="text-xl font-semibold text-gray-900">{value}</div>
        </div>
      </div>
      {delta && <div className="text-sm text-green-600 font-medium">{delta}</div>}
    </div>
  );
}
