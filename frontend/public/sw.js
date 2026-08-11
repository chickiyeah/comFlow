/* CampusFlow Service Worker — Web Push */
self.addEventListener('push', (event) => {
  let data = {}
  try { data = event.data ? event.data.json() : {} }
  catch (e) { data = { body: event.data ? event.data.text() : '' } }
  const title = data.title || 'CampusFlow'
  const options = {
    body: data.body || '',
    data: { url: data.url || '/' },
    tag: data.tag || undefined,
  }
  event.waitUntil(self.registration.showNotification(title, options))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = (event.notification.data && event.notification.data.url) || '/'
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((list) => {
      for (const c of list) {
        if ('focus' in c) { c.focus(); if (c.navigate && url) { try { c.navigate(url) } catch (e) {} } return }
      }
      if (clients.openWindow) return clients.openWindow(url)
    })
  )
})
