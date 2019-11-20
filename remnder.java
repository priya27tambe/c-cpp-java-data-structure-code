	@RequestMapping(path = "/addReminderOfEvent", method = RequestMethod.POST)
	public ResponseEntity<List<RfqReminder>> addReminderOfEvent(@RequestParam(value = "reminderDuration") String reminderDuration, @RequestParam(value = "dateRangeData") String dateRangeData, @RequestParam(value = "reminderDurationType") IntervalType reminderDurationType, @RequestParam(value = "eventId") String eventId, @RequestParam(value = "reminderId") String reminderId, @RequestParam(value = "reminderNotifyType") String reminderNotifyType, Model model, HttpSession session) {
		HttpHeaders headers = new HttpHeaders();
		try {
			TimeZone timeZone = TimeZone.getDefault();
			String strTimeZone = (String) session.getAttribute(Global.SESSION_TIME_ZONE_KEY);
			if (strTimeZone != null) {
				timeZone = TimeZone.getTimeZone(strTimeZone);
			}
			if (StringUtils.checkString(eventId).length() > 0) {
				RfqEvent event = rfqEventService.getEventById(eventId);
				if (StringUtils.checkString(reminderId).length() == 0) {
					RfqReminder reminder = new RfqReminder();
					if (StringUtils.checkString(dateRangeData).length() > 0) {
						String visibilityDates[] = dateRangeData.split("-");
						DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm a");
						formatter.setTimeZone(timeZone);
						Date startDate = (Date) formatter.parse(visibilityDates[0]);
						Date endDate = (Date) formatter.parse(visibilityDates[1]);

						Calendar cal = Calendar.getInstance(timeZone);
						if (StringUtils.checkString(reminderNotifyType).equalsIgnoreCase("Start")) {
							cal.setTime(startDate);
							reminder.setStartReminder(Boolean.TRUE);
						} else {
							cal.setTime(endDate);
						}
						if (reminderDurationType == IntervalType.DAYS) {
							cal.add(Calendar.DATE, -Integer.parseInt(reminderDuration));
							reminder.setIntervalType(IntervalType.DAYS);
						} else {
							cal.add(Calendar.HOUR, -Integer.parseInt(reminderDuration));
							reminder.setIntervalType(IntervalType.HOURS);
						}
						reminder.setReminderDate(cal.getTime());
						List<RfqReminder> reminderList = rfqEventService.getAllRfqEventReminderForEvent(eventId);
						if (CollectionUtil.isNotEmpty(reminderList)) {
							for (RfqReminder rfqReminderCompare : reminderList) {
								if (reminder.getReminderDate().compareTo(rfqReminderCompare.getReminderDate()) == 0 && (reminder.getStartReminder() == rfqReminderCompare.getStartReminder())) {
									headers.add("error", "There is another reminder on this date is exists");
									return new ResponseEntity<List<RfqReminder>>(null, headers, HttpStatus.INTERNAL_SERVER_ERROR);
								}
							}
						}
						// if (reminder.getReminderDate().before(startDate)) {
						// headers.add("error", "Reminder date/time should not be less than event start date");
						// return new ResponseEntity<List<RfqReminder>>(null, headers,
						// HttpStatus.INTERNAL_SERVER_ERROR);
						// }
						if (cal.getTime().compareTo(new Date()) < 0) {
							headers.add("error", messageSource.getMessage("event.reminder.pastdate", new Object[] {}, Global.LOCALE));
							return new ResponseEntity<List<RfqReminder>>(null, headers, HttpStatus.INTERNAL_SERVER_ERROR);
						}
						event.setEventStart(startDate);
						event.setEventEnd(endDate);
						rfqEventService.updateEvent(event);

						reminder.setRfxEvent(event);
						reminder.setInterval(Integer.parseInt(reminderDuration));
						LOG.info(reminder);
						rfqEventService.saveEventReminder(reminder);
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Error While Save the Reminder" + e.getMessage(), e);
			return new ResponseEntity<List<RfqReminder>>(null, headers, HttpStatus.EXPECTATION_FAILED);
		}
		return new ResponseEntity<List<RfqReminder>>(rfqEventService.getAllEventReminderForEvent(eventId), HttpStatus.OK);
	}

