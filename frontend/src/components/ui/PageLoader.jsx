/** Full-area spinner shown while a lazy-loaded page is downloading. */
export default function PageLoader() {
  return (
    <div className="flex min-h-[50vh] items-center justify-center" role="status" aria-label="Loading">
      <span className="size-10 animate-spin rounded-full border-4 border-gold-400 border-t-transparent" />
    </div>
  );
}
