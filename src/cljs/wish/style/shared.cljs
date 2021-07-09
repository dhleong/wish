(ns ^{:author "Daniel Leong"
      :doc "Shared inline styles"}
  wish.style.shared)

(def metadata {:font-size "10pt"
               :font-weight 'normal})

(def unselectable {:-webkit-user-select 'none
                   :user-select 'none})
(def clickable (assoc unselectable
                      :cursor 'pointer))
