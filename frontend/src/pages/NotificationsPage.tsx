import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { notificationsApi } from '../api/endpoints'
import type { Notification } from '../api/types'
import { Alert, Button, Card, EmptyState, PageHeader, PageLoader } from '../components/ui'
import { errorMessage } from '../lib/ui-helpers'
import { formatDateTime, timeAgo } from '../lib/format'

const icons: Record<string, string> = {
  LOAN_APPROVED: '✅', LOAN_REJECTED: '✖️', DUE_SOON: '⏰', OVERDUE: '⚠️', RESERVATION_READY: '📚', HOLD_EXPIRED: '⌛',
}

export function NotificationsPage() {
  const queryClient = useQueryClient()
  const [unreadOnly, setUnreadOnly] = useState(false)
  const list = useQuery({ queryKey: ['notifications', 'list', unreadOnly], queryFn: () => notificationsApi.list(unreadOnly) })

  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['notifications'] })
  const markRead = useMutation({ mutationFn: (id: number) => notificationsApi.markRead(id), onSuccess: refresh })
  const markAll = useMutation({ mutationFn: notificationsApi.markAllRead, onSuccess: refresh })

  return (
    <>
      <PageHeader
        title="Notifications"
        subtitle="Approvals, reminders and reservation updates"
        actions={
          <div className="flex items-center gap-3">
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" className="size-4 rounded border-slate-300" checked={unreadOnly} onChange={(e) => setUnreadOnly(e.target.checked)} />
              Unread only
            </label>
            <Button variant="secondary" loading={markAll.isPending} onClick={() => markAll.mutate()}>Mark all read</Button>
          </div>
        }
      />

      {list.isPending ? <PageLoader />
        : list.isError ? <Alert>{errorMessage(list.error)}</Alert>
          : list.data.content.length === 0 ? <EmptyState title={unreadOnly ? "You're all caught up" : 'No notifications yet'} />
            : (
              <Card className="divide-y divide-slate-100">
                {list.data.content.map((n) => <Row key={n.id} n={n} onRead={() => markRead.mutate(n.id)} />)}
              </Card>
            )}
    </>
  )
}

function Row({ n, onRead }: { n: Notification; onRead: () => void }) {
  return (
    <div className={`flex gap-4 px-4 py-4 ${n.read ? '' : 'bg-indigo-50/50'}`}>
      <span className="text-xl" aria-hidden>{icons[n.type] ?? '🔔'}</span>
      <div className="min-w-0 flex-1">
        <p className={`text-sm ${n.read ? 'text-slate-700' : 'font-semibold text-slate-900'}`}>{n.title}</p>
        <p className="mt-0.5 text-sm text-slate-500">{n.message}</p>
        <p className="mt-1 text-xs text-slate-400" title={formatDateTime(n.createdAt)}>{timeAgo(n.createdAt)}</p>
      </div>
      {!n.read && (
        <button onClick={onRead} className="self-start text-xs font-medium text-indigo-600 hover:underline">Mark read</button>
      )}
    </div>
  )
}
