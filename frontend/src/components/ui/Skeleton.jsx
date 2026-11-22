/** Grey shimmer block used while data is loading. */
export default function Skeleton({ className = '' }) {
  return <div className={`animate-pulse rounded-xl bg-slate-200/80 ${className}`} aria-hidden="true" />;
}

/** Skeleton shaped like a ProductCard. */
export function ProductCardSkeleton() {
  return (
    <div className="overflow-hidden rounded-2xl bg-white p-3 shadow-sm ring-1 ring-slate-100">
      <Skeleton className="aspect-square w-full" />
      <Skeleton className="mt-3 h-3 w-1/3" />
      <Skeleton className="mt-2 h-4 w-4/5" />
      <Skeleton className="mt-2 h-3 w-1/2" />
      <Skeleton className="mt-4 h-9 w-full rounded-full" />
    </div>
  );
}
