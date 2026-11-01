import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { notificationsApi } from '../api/endpoints'
import { NotificationIcon } from '../components/NotificationIcon'
import type { Notification } from '../api/types'
import { Alert, Button, Card, EmptyState, PageHeader, PageLoader } from '../components/ui'
import { errorMessage } from '../lib/ui-helpers'
import { formatDateTime, timeAgo } from '../lib/format'


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
            <label className="flex items-center gap-2 text-sm text-ink-700">
              <input type="checkbox" className="size-4 rounded border-ink-300" checked={unreadOnly} onChange={(e) => setUnreadOnly(e.target.checked)} />
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
              <Card className="divide-y divide-ink-100">
                {list.data.content.map((n) => <Row key={n.id} n={n} onRead={() => markRead.mutate(n.id)} />)}
              </Card>
            )}
    </>
  )
}

function Row({ n, onRead }: { n: Notification; onRead: () => void }) {
  return (
    <div className="relative flex gap-4 px-4 py-4">
      {!n.read && <span aria-label="Unread" className="absolute top-1/2 left-1.5 size-2 -translate-y-1/2 rounded-full bg-accent-500" />}
      <NotificationIcon type={n.type} />
      <div className="min-w-0 flex-1">
        <p className={`text-sm ${n.read ? 'text-ink-700' : 'font-semibold text-ink-900'}`}>{n.title}</p>
        <p className="mt-0.5 text-sm text-ink-500">{n.message}</p>
        <p className="mt-1 text-xs text-ink-500" title={formatDateTime(n.createdAt)}>{timeAgo(n.createdAt)}</p>
      </div>
      {!n.read && (
        <button onClick={onRead} className="self-start text-xs font-medium text-accent-600 hover:underline">Mark read</button>
      )}
    </div>
  )
}
