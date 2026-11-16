# ADR 0007: Frontend: MVVM com Next.js 14 App Router

## Status
Accepted

## Contexto
Portal web multi-perfil (Admin SaaS, Gestor, Recepção, Aluno).
Requisitos:
- SEO/SSR para páginas públicas (login, landing, LGPD)
- React Server Components para performance
- Type safety end-to-end (shared contracts com backend)
- Design System consistente
- PWA para aluno (offline-first básico)

## Decisão
**Next.js 14+ (App Router) + TypeScript + MVVM (ViewModel pattern)**:

### Arquitetura de Camadas
```
app/                    # Next.js App Router (Routing, Layouts, Server Components)
├── (auth)/             # Route groups
├── (dashboard)/
│   ├── admin/          # Admin SaaS (Server Components + Client islands)
│   ├── manager/        # Gestor Academia
│   ├── reception/      # Recepção (Client Components pesados)
│   └── student/        # Aluno (PWA, offline-capable)
components/
├── ui/                 # Design System (shadcn/ui + Tailwind)
├── forms/              # Form components (React Hook Form + Zod)
└── charts/             # Recharts wrappers
lib/
├── api/                # Client SDK (fetch + Zod validation + TanStack Query)
├── auth/               # Auth context, token refresh, PKCE
├── store/              # Zustand (UI state: modals, sidebars, filters)
└── utils/
viewmodels/             # ViewModels por feature (lógica de apresentação)
├── student/
│   ├── StudentListViewModel.ts
│   └── StudentDetailViewModel.ts
├── access/
└── device/
types/                  # Zod schemas + TS types (gerados do OpenAPI backend)
```

### ViewModel Pattern (Exemplo)
```typescript
// viewmodels/student/StudentListViewModel.ts
export class StudentListViewModel {
  // State (signals para fine-grained reactivity)
  private _students = signal<Student[]>([]);
  private _loading = signal(false);
  private _filters = signal<StudentFilters>({ status: 'ACTIVE' });
  private _sort = signal<SortConfig>({ field: 'name', dir: 'asc' });

  // Dependencies (injected)
  constructor(
    private api: ApiClient,
    private store: StudentStore,
    private router: Router
  ) {}

  // Computed (derived state)
  readonly students = computed(() => 
    this._students().filter(s => matchesFilters(s, this._filters()))
  );
  readonly loading = this._loading.asReadonly();
  readonly totalCount = computed(() => this._students().length);

  // Actions
  async load() {
    this._loading.set(true);
    try {
      const data = await this.api.students.list({ 
        filters: this._filters(),
        sort: this._sort()
      });
      this._students.set(data);
      this.store.setStudents(data); // Sync global store if needed
    } finally {
      this._loading.set(false);
    }
  }

  async enrollBiometric(studentId: string, image: File) {
    const result = await this.api.biometric.enroll(studentId, image);
    if (result.success) {
      await this.load(); // Refresh
      toast.success('Biometria cadastrada');
    }
  }
}
```

### Server Components por Default
- Listagens, dashboards, relatórios → **Server Components** (fetch direto no backend, zero JS client)
- Interatividade (modais, forms, tempo real) → **Client Components** (`'use client'`) isolados

## Consequências

### Positivas
- **Performance**: RSC = menos JS, streaming SSR, cache granular
- **Type Safety**: Zod schemas single source of truth (backend OpenAPI → frontend types)
- **Separation of Concerns**: ViewModel encapsula lógica de UI; Components são "dumb"
- **Testability**: ViewModels testáveis unitariamente (sem React); Components testados com RTL
- **PWA Ready**: Service Worker + Cache API para aluno offline

### Negativas
- **Curva de aprendizado**: RSC + Server Actions + Suspense patterns
- **Bundle size**: Next.js runtime overhead (~50KB gz) vs Vite SPA
- **Server Components limitations**: Não usam hooks, browser APIs — requer boundary claro

### Riscos
- **Over-fetching** em Server Components → **Mitigação**: `fetch` com `next: { tags }` + revalidação granular
- **Hydration mismatches** → **Mitigação**: `suppressHydrationWarning` só onde necessário; evitar date/Random no server
- **TanStack Query + RSC** overlap → **Mitigação**: RSC para dados iniciais; TanStack Query para mutações/client caching

## Alternativas Consideradas
1. **React SPA (Vite + TanStack Router)**: Sem SSR/SEO; mais JS; PWA manual. Rejeitado.
2. **Remix**: Excelente, mas ecossistema menor; Next.js tem mais componentes/libraries. Rejeitado.
3. **Vue 3 + Nuxt 3**: Time é React-native; curva de aprendizado. Rejeitado.
4. **Traditional MVC (Thymeleaf)**: Sem interatividade rica; não atende recepção/aluno. Rejeitado.

## Referências
- Next.js 14 App Router: https://nextjs.org/docs/app
- "MVVM in React" - various patterns (signals, Zustand, Jotai)
- shadcn/ui: https://ui.shadcn.com/
- TanStack Query: https://tanstack.com/query
- OpenAPI → TypeScript: `openapi-typescript` + `orval`