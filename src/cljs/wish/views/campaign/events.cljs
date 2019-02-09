(ns ^{:author "Daniel Leong"
      :doc "Campaign-specific events"}
  wish.views.campaign.events
  (:require-macros [wish.util.log :as log :refer [log]])
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx
                                   path
                                   inject-cofx trim-v]]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [vimsical.re-frame.cofx.inject :as inject]
            [wish.sheets.util :refer [update-sheet-path]]))

;; can also be passed nil to leave a campaign
(reg-event-fx
  ::join-campaign
  [trim-v (inject-cofx ::inject/sub [:active-sheet-id])]
  (fn [{:keys [active-sheet-id] :as cofx} [campaign-id ?campaign-name]]
    (if campaign-id
      (let [info {:id campaign-id
                  :name ?campaign-name}]
        (log/info "Joining " campaign-id)

        (-> cofx
            (update-sheet-path [] assoc :campaign info)

            ; raise a notifier
            (assoc :dispatch [:notify! {:duration :short
                                        :content (str "Joined "
                                                      (or (:name info)
                                                          "the campaign")
                                                      " successfully!")}])))

      (do
        (log/info "Leaving campaign")

        (-> cofx
            (update-sheet-path [] dissoc :campaign)

            ; raise a notifier
            (assoc :dispatch [:notify! {:duration :short
                                       :content "Left the campaign successfully."}]))))))

(reg-event-fx
  ::add-player
  [trim-v]
  (fn-traced [cofx [character-id]]
    (update-sheet-path cofx [:players] conj character-id)))

(reg-event-fx
  ::remove-player
  [trim-v]
  (fn-traced [cofx [character-id]]
    (update-sheet-path cofx [:players] disj character-id)))

