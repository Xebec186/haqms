import Header from './Header';

export default function PageWrapper({ children, title, subtitle, action }) {
  return (
    <div className="min-h-screen pt-2">
      <Header />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 md:py-12">
        {(title || action) && (
          <div className="flex items-start justify-between gap-6 mb-8 flex-wrap">
            <div>
              {title && (
                <h1 className="text-4xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-slate-900 to-indigo-900 tracking-tight mb-2">
                  {title}
                </h1>
              )}
              {subtitle && (
                <p className="text-lg text-slate-600 max-w-2xl">{subtitle}</p>
              )}
            </div>
            {action && <div className="shrink-0 mt-2">{action}</div>}
          </div>
        )}
        <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
          {children}
        </div>
      </main>
    </div>
  );
}
