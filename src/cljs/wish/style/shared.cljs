(ns ^{:author "Daniel Leong"
      :doc "Shared inline styles"}
  wish.style.shared)

(def metadata {:font-size "10pt"
               :font-weight 'normal})

(def clickable {:cursor 'pointer})
(def unselectable {:-webkit-user-select 'none
                   :user-select 'none})
